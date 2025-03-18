# Technical Details

This document provides technical information about how AndroidDisplay works and its architecture.

## System Architecture

AndroidDisplay consists of two main components:

1. **Fedora Component**: Creates a virtual display, captures its contents, and streams it to the Android tablet
2. **Android App**: Receives the video stream and displays it, while sending touch events back to the Fedora system

### Overall Data Flow

```
+---------------+                      +----------------+
|               |  Video Stream (UDP)  |                |
|   Fedora      | -------------------> |    Android     |
|   System      |                      |    Tablet      |
|   (KDE/X11)   |                      |                |
|               | <------------------- |                |
|               |  Touch Events (TCP)  |                |
+---------------+                      +----------------+
```

## Fedora Component Details

### Virtual Display Creation

The virtual display is created using the `xrandr` utility and the X11 dummy driver. The `virtual-display.sh` script:

1. Finds the primary display
2. Locates an available virtual output
3. Creates a new mode with the desired resolution
4. Enables the virtual output with the new mode
5. Positions it relative to the primary display

### Video Streaming

The `stream-display.sh` script:

1. Captures the virtual display using FFmpeg's x11grab
2. Compresses it using H.264 encoding (hardware-accelerated via VA-API when available)
3. Streams the compressed video over UDP to the Android tablet
4. Adaptive quality settings based on network conditions

### Input Handling

The `input-handler.py` script:

1. Listens for touch events from the Android tablet over TCP
2. Maps touch coordinates based on the virtual display's position
3. Injects mouse events into X11 using `xdotool`
4. Supports basic touch gestures (down, up, move)

### Automatic Device Detection

The system uses udev rules to detect when the tablet is connected:

1. The rule matches the specific USB vendor and product IDs of the tablet
2. When connected, it automatically starts the AndroidDisplay service
3. When disconnected, it stops the service

## Android App Details

### Video Decoding

The Android app:

1. Sets up a SurfaceView to display the video
2. Creates a MediaCodec decoder for H.264 video
3. Listens for UDP packets containing the video stream
4. Feeds the packets to the decoder
5. Renders the decoded frames to the SurfaceView

### Touch Event Handling

The app captures touch events and sends them to the Fedora system:

1. The SurfaceView captures MotionEvents (DOWN, UP, MOVE)
2. These events are packaged into a compact binary format:
   - 1 byte for action type
   - 4 bytes for X coordinate (int)
   - 4 bytes for Y coordinate (int)
3. The packaged events are sent over TCP to the input handler on Fedora

### Network Configuration

The app uses USB tethering for optimal performance:

1. When the tablet is connected via USB and tethering is enabled, it creates a direct network connection
2. The Fedora system typically has the IP address 192.168.42.1
3. The tablet typically has the IP address 192.168.42.129
4. This direct connection minimizes latency compared to going through Wi-Fi

## Protocol Details

### Video Protocol

The video streaming uses standard MPEG-TS over UDP:

- Port: 5000
- Transport: UDP (for lower latency)
- Codec: H.264 (hardware accelerated when possible)
- Container: MPEG-TS
- Resolution: Configurable (default 1600x1200)
- Framerate: Configurable (default 30fps)
- Bitrate: Adaptive based on quality setting (1.5-5 Mbps)

### Touch Protocol

Touch events use a custom binary protocol over TCP:

- Port: 5001
- Transport: TCP (for reliability)
- Message format:
  - Byte 0: Event type (0=DOWN, 1=UP, 2=MOVE)
  - Bytes 1-4: X coordinate (32-bit integer, big-endian)
  - Bytes 5-8: Y coordinate (32-bit integer, big-endian)

## Performance Considerations

### Latency Optimization

To minimize latency:

1. Video encoding uses low-latency presets
2. Hardware acceleration is used when available
3. UDP is used for video to minimize overhead
4. The USB connection provides higher bandwidth and lower latency than Wi-Fi
5. Frame buffer is kept small to reduce delay

### Resolution and Quality

The system balances quality and performance:

1. Default resolution is 1600x1200 (Lenovo P11 native is 2000x1200)
2. Three quality presets available (low, medium, high)
3. Bitrate is adjusted based on the quality setting
4. Hardware accelerated encoding/decoding reduces CPU load

## Security Considerations

1. The service only runs when the tablet is connected via USB
2. The video and input services only listen on the USB network interface
3. No authentication is currently implemented since the connection is direct via USB
4. Future versions could implement TLS encryption for the TCP connection