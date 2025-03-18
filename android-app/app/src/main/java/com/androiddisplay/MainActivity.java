package com.androiddisplay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_SERVER_IP = "server_ip";
    
    private SurfaceView surfaceView;
    private LinearLayout controlsLayout;
    private FloatingActionButton showControlsFab;
    private TextView statusText;
    private EditText ipAddressField;
    private Button connectButton;
    private Button hideControlsButton;
    
    private boolean isConnected = false;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Initialize preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Initialize views
        surfaceView = findViewById(R.id.surfaceView);
        controlsLayout = findViewById(R.id.controlsLayout);
        showControlsFab = findViewById(R.id.showControlsFab);
        statusText = findViewById(R.id.statusText);
        ipAddressField = findViewById(R.id.ipAddressField);
        connectButton = findViewById(R.id.connectButton);
        hideControlsButton = findViewById(R.id.hideControlsButton);
        
        // Set initial state
        String savedIp = prefs.getString(PREF_SERVER_IP, "192.168.42.1");
        ipAddressField.setText(savedIp);
        
        // Set up click listeners
        connectButton.setOnClickListener(v -> toggleConnection());
        hideControlsButton.setOnClickListener(v -> hideControls());
        showControlsFab.setOnClickListener(v -> showControls());
        
        // Set up touch event handling
        surfaceView.setOnTouchListener((v, event) -> {
            if (isConnected) {
                handleTouchEvent(event);
            }
            return true;
        });
    }
    
    private void toggleConnection() {
        if (isConnected) {
            // Disconnect
            stopService(new Intent(this, DisplayService.class));
            isConnected = false;
            statusText.setText(R.string.status_disconnected);
            connectButton.setText(R.string.connect);
        } else {
            // Connect
            String serverIp = ipAddressField.getText().toString().trim();
            if (!serverIp.isEmpty()) {
                // Save IP address
                prefs.edit().putString(PREF_SERVER_IP, serverIp).apply();
                
                // Start service
                Intent intent = new Intent(this, DisplayService.class);
                intent.putExtra(DisplayService.EXTRA_SERVER_IP, serverIp);
                startService(intent);
                
                isConnected = true;
                statusText.setText(R.string.status_connected);
                connectButton.setText(R.string.disconnect);
                
                // Hide controls after connecting
                hideControls();
            }
        }
    }
    
    private void hideControls() {
        controlsLayout.setVisibility(View.GONE);
        showControlsFab.setVisibility(View.VISIBLE);
    }
    
    private void showControls() {
        controlsLayout.setVisibility(View.VISIBLE);
        showControlsFab.setVisibility(View.GONE);
    }
    
    private void handleTouchEvent(MotionEvent event) {
        // Get display dimensions
        int width = surfaceView.getWidth();
        int height = surfaceView.getHeight();
        
        // Only handle if we have dimensions
        if (width <= 0 || height <= 0) return;
        
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        
        // Map action to the constants expected by the server
        int mappedAction;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mappedAction = 0; // Touch down
                break;
            case MotionEvent.ACTION_UP:
                mappedAction = 1; // Touch up
                break;
            case MotionEvent.ACTION_MOVE:
                mappedAction = 2; // Touch move
                break;
            default:
                return;
        }
        
        // Send the touch event to the service
        Intent intent = new Intent(this, DisplayService.class);
        intent.setAction(DisplayService.ACTION_TOUCH_EVENT);
        intent.putExtra(DisplayService.EXTRA_TOUCH_ACTION, mappedAction);
        intent.putExtra(DisplayService.EXTRA_TOUCH_X, (int) x);
        intent.putExtra(DisplayService.EXTRA_TOUCH_Y, (int) y);
        startService(intent);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Stop the service when the activity is destroyed
        if (isConnected) {
            stopService(new Intent(this, DisplayService.class));
        }
    }
}