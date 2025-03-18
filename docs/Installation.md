# Installation Guide

This guide explains how to set up AndroidDisplay to use your Lenovo P11 tablet as an external monitor for your Fedora 41 KDE/X11 system.

## Prerequisites

### Fedora System
- Fedora 41 with KDE using X11 (not Wayland)
- Root/sudo access
- Required packages: ffmpeg, xrandr, xdotool, libva-utils, python3

### Android Tablet
- Lenovo P11 (or other Android tablet) with Android 11+
- USB-C cable for connecting to the Fedora system
- Developer options enabled

## Installing the Fedora Component

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/AndroidDisplay.git
   cd AndroidDisplay
   ```

2. Run the setup script as root:
   ```bash
   cd fedora-app
   sudo ./setup.sh
   ```

   This will:
   - Install required packages
   - Make scripts executable
   - Install the systemd service
   - Set up the udev rule
   - Configure X11 for the virtual display

3. Update the udev rule with your tablet's vendor and product IDs:
   ```bash
   # Connect your tablet via USB
   lsusb  # Find your tablet in the list
   sudo nano /etc/udev/rules.d/99-lenovo-p11.rules  # Update IDs
   sudo udevadm control --reload-rules
   ```

## Installing the Android App

1. Install Android Studio on your Fedora system:
   ```bash
   sudo dnf install android-studio
   ```

2. Open the Android app project in Android Studio:
   ```bash
   android-studio AndroidDisplay/android-app/
   ```

3. Connect your tablet to your computer with USB debugging enabled.

4. Click "Run" in Android Studio to build and install the app on your tablet.

## Using AndroidDisplay

1. Connect your Lenovo P11 tablet to your Fedora system using a USB-C cable.

2. Enable USB tethering on your tablet:
   - Go to Settings > Network & Internet > Hotspot & tethering
   - Enable "USB tethering"

3. The service should start automatically when the tablet is connected (thanks to the udev rule).

4. If it doesn't start automatically:
   ```bash
   sudo systemctl start android-display.service
   ```

5. On your tablet, open the AndroidDisplay app.

6. Enter the IP address of your Fedora system (usually 192.168.42.1 when using USB tethering).

7. Tap "Connect".

8. You should now see your virtual display on the tablet, and your Fedora system should treat it as an additional monitor.

## Troubleshooting

### No display on tablet
- Check that the virtual display is created correctly:
  ```bash
  xrandr --listactivemonitors
  ```
- Verify the streaming service is running:
  ```bash
  systemctl status android-display.service
  ```

### Touch input not working
- Check the input handler is running:
  ```bash
  ps aux | grep input-handler.py
  ```
- Verify the IP address in the app is correct

### Virtual display not created
- Check X11 is running (not Wayland):
  ```bash
  echo $XDG_SESSION_TYPE
  ```
- Install xorg-x11-drv-dummy if not already installed:
  ```bash
  sudo dnf install xorg-x11-drv-dummy
  ```

### Connection problems
- Check USB tethering is enabled on the tablet
- Verify the network connection between the tablet and computer:
  ```bash
  ping 192.168.42.129  # Default Android USB tethering IP
  ```