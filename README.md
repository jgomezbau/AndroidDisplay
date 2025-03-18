# AndroidDisplay
# Proyecto en desarrollo no funciona aun

A hybrid application (Fedora + Android) for using a Lenovo P11 tablet as an external monitor with a Fedora 41 KDE/X11 system via USB-C connection.

![AndroidDisplay](https://placeholder-for-your-logo.com/androidisplay-logo.png)

## 🎯 Project Overview

AndroidDisplay solves the problem of using a tablet as an external monitor for Linux systems when DisplayPort Alt Mode is not supported. It creates a virtual display in Fedora that is streamed to an Android tablet, effectively turning the tablet into an external monitor with full touch input support.

### 🌟 Key Features

- **Virtual Monitor**: Creates an actual X11 virtual display that appears as a real monitor in your system
- **Touch Support**: Translates touch events on the tablet to mouse events on Fedora
- **Low Latency**: Optimized for minimal delay (<100ms) over USB-C connection
- **Hardware Acceleration**: Uses GPU acceleration for video encoding/decoding when available
- **Auto Detection**: Automatically detects when the tablet is connected via USB
- **Fallback Modes**: Supports Wi-Fi connection if USB is unavailable

## 🛠️ Components

### Fedora Component
- Virtual display creation using xrandr and X11 dummy driver
- Video streaming using FFmpeg with hardware acceleration
- Input handling for touch events
- Automatic device detection and service management

### Android App
- H.264 video decoding using Android MediaCodec
- Touch event capture and forwarding
- User-friendly interface with connection management
- Background service for continuous operation

## 📋 Requirements

### Fedora (Linux)
- Fedora 41 with KDE/X11 (not Wayland)
- FFmpeg with VA-API support (for hardware acceleration)
- xrandr and xorg-x11-drv-dummy packages
- Python 3.6+ with socket support

### Android
- Android 11+ device (tested with Lenovo P11)
- USB-C connection (must support USB tethering)
- MediaCodec API support (for hardware decoding)

## 📚 Documentation

- [Quick Start Guide](QuickStart.md) - Get up and running in minutes
- [Installation Guide](docs/Installation.md) - Detailed installation instructions
- [Technical Details](docs/TechnicalDetails.md) - Architecture and protocol information
- [Changelog](CHANGELOG.md) - Version history and changes

## 🚀 Getting Started

1. Clone this repository
2. Set up the Fedora component: `sudo ./fedora-app/setup.sh`
3. Install the Android app on your tablet
4. Connect your tablet via USB-C and enable USB tethering
5. Launch the app and connect to your Fedora system

See the [Quick Start Guide](QuickStart.md) for more detailed instructions.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📜 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- FFmpeg for video encoding/streaming capabilities
- Android MediaCodec for hardware-accelerated decoding
- X11 for virtual display support