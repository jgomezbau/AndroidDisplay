package com.androiddisplay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DisplayService extends Service {

    private static final String TAG = "DisplayService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "DisplayServiceChannel";
    
    // Constants for touch events
    public static final String ACTION_TOUCH_EVENT = "com.androiddisplay.ACTION_TOUCH_EVENT";
    public static final String EXTRA_TOUCH_ACTION = "touch_action";
    public static final String EXTRA_TOUCH_X = "touch_x";
    public static final String EXTRA_TOUCH_Y = "touch_y";
    public static final String EXTRA_SERVER_IP = "server_ip";
    
    // Default ports
    private static final int VIDEO_PORT = 5000;
    private static final int INPUT_PORT = 5001;
    
    // Service state
    private boolean isRunning = false;
    private String serverIp;
    private ExecutorService executorService;
    private DatagramSocket videoSocket;
    private Socket inputSocket;
    private OutputStream inputOutputStream;
    private Handler mainHandler;
    
    // Media components
    private MediaCodec decoder;
    private Surface outputSurface;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    
    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newFixedThreadPool(3);
        
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        
        String action = intent.getAction();
        
        // If this is a touch event and we're already running, handle it
        if (ACTION_TOUCH_EVENT.equals(action) && isRunning) {
            handleTouchEvent(
                intent.getIntExtra(EXTRA_TOUCH_ACTION, 0),
                intent.getIntExtra(EXTRA_TOUCH_X, 0),
                intent.getIntExtra(EXTRA_TOUCH_Y, 0)
            );
            return START_STICKY;
        }
        
        // Otherwise, start or configure the service
        if (!isRunning) {
            serverIp = intent.getStringExtra(EXTRA_SERVER_IP);
            if (serverIp != null && !serverIp.isEmpty()) {
                startForeground(NOTIFICATION_ID, createNotification());
                startDisplayService();
            }
        }
        
        return START_STICKY;
    }
    
    private void startDisplayService() {
        isRunning = true;
        
        // Get the SurfaceView from MainActivity
        surfaceView = ((MainActivity) getApplicationContext()).findViewById(R.id.surfaceView);
        if (surfaceView == null) {
            showToast("Error: SurfaceView not found");
            stopSelf();
            return;
        }
        
        surfaceHolder = surfaceView.getHolder();
        
        // Start a thread to handle the video stream
        executorService.execute(this::receiveVideoStream);
        
        // Start a thread to handle the input connection
        executorService.execute(this::connectInputSocket);
        
        showToast("Connected to " + serverIp);
    }
    
    private void receiveVideoStream() {
        try {
            videoSocket = new DatagramSocket(VIDEO_PORT);
            videoSocket.setReceiveBufferSize(500000);
            
            // Wait for the surface to be valid
            while (surfaceHolder.getSurface() == null || !surfaceHolder.getSurface().isValid()) {
                Thread.sleep(100);
            }
            
            // Initialize the codec
            initializeCodec();
            
            // Receive and decode video
            receiveAndDecodeVideo();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in video stream thread", e);
            showToast("Video stream error: " + e.getMessage());
            stopSelf();
        }
    }
    
    private void initializeCodec() throws IOException {
        // Create a decoder for H.264 video
        decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        
        // Configure the decoder
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1600, 1200);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 500000);
        
        // Set up the output surface
        outputSurface = surfaceHolder.getSurface();
        decoder.configure(format, outputSurface, null, 0);
        decoder.start();
    }
    
    private void receiveAndDecodeVideo() {
        final int BUFFER_SIZE = 65536;
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        int inputBufferIndex;
        ByteBuffer inputBuffer;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        
        try {
            while (isRunning) {
                // Receive UDP packet
                videoSocket.receive(packet);
                
                // Get an input buffer from the decoder
                inputBufferIndex = decoder.dequeueInputBuffer(10000);
                if (inputBufferIndex >= 0) {
                    inputBuffer = decoder.getInputBuffer(inputBufferIndex);
                    if (inputBuffer != null) {
                        inputBuffer.clear();
                        inputBuffer.put(buffer, 0, packet.getLength());
                        
                        // Queue the buffer for decoding
                        decoder.queueInputBuffer(inputBufferIndex, 0, packet.getLength(), System.nanoTime() / 1000, 0);
                    }
                }
                
                // Handle decoded output
                int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000);
                while (outputBufferIndex >= 0) {
                    decoder.releaseOutputBuffer(outputBufferIndex, true);
                    outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error receiving video", e);
        }
    }
    
    private void connectInputSocket() {
        try {
            inputSocket = new Socket();
            inputSocket.connect(new InetSocketAddress(serverIp, INPUT_PORT), 5000);
            inputOutputStream = inputSocket.getOutputStream();
            Log.d(TAG, "Input socket connected to " + serverIp + ":" + INPUT_PORT);
        } catch (Exception e) {
            Log.e(TAG, "Error connecting input socket", e);
            showToast("Input connection error: " + e.getMessage());
        }
    }
    
    private void handleTouchEvent(int action, int x, int y) {
        if (inputOutputStream != null) {
            try {
                // Create a buffer for the touch event data (9 bytes)
                ByteBuffer buffer = ByteBuffer.allocate(9);
                buffer.order(ByteOrder.BIG_ENDIAN);
                
                // Write action and coordinates to the buffer
                buffer.put((byte) action);
                buffer.putInt(x);
                buffer.putInt(y);
                
                // Send the buffer through the socket
                buffer.flip();
                inputOutputStream.write(buffer.array());
                inputOutputStream.flush();
            } catch (Exception e) {
                Log.e(TAG, "Error sending touch event", e);
            }
        }
    }
    
    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }
    
    private void releaseResources() {
        try {
            if (decoder != null) {
                decoder.stop();
                decoder.release();
                decoder = null;
            }
            
            if (videoSocket != null) {
                videoSocket.close();
                videoSocket = null;
            }
            
            if (inputOutputStream != null) {
                inputOutputStream.close();
                inputOutputStream = null;
            }
            
            if (inputSocket != null) {
                inputSocket.close();
                inputSocket = null;
            }
            
            if (executorService != null) {
                executorService.shutdown();
                executorService = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing resources", e);
        }
    }

    // Create notification channel for Android 8.0 and above
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.channel_description));
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    // Create notification for foreground service
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        releaseResources();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}