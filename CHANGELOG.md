# Changelog

All notable changes to the AndroidDisplay project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2025-03-10

### Added
- Initial project structure
- Fedora component with:
  - Virtual display creation script (virtual-display.sh)
  - Display streaming script (stream-display.sh)
  - Input handling script (input-handler.py)
  - Systemd service configuration
  - udev rules for automatic detection
  - Setup script
- Android app with:
  - Video stream receiver
  - H.264 decoding
  - Touch event capture and forwarding
  - Connection management UI
- Documentation:
  - Installation guide
  - Technical documentation
  - Quick start guide
  - Changelog

### Technical Features
- X11 virtual display creation using xrandr
- Hardware-accelerated video encoding (VA-API) when available
- H.264 video streaming over UDP
- Touch event handling with coordinate mapping
- USB tethering for optimal connectivity
- Automatic service management via systemd
- Foreground service on Android