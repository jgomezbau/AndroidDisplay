# AndroidDisplay - Quick Start Guide

This guide provides the quickest way to get up and running with AndroidDisplay.

## Fedora Setup (One-time)

1. Install required packages:
   ```bash
   sudo dnf install ffmpeg xrandr xdotool libva-utils python3 python3-pip
   ```

2. Install X11 dummy driver:
   ```bash
   sudo dnf install xorg-x11-drv-dummy
   ```

3. Make sure you're running X11 (not Wayland):
   ```bash
   echo $XDG_SESSION_TYPE
   ```
   This should output `x11`. If it shows `wayland`, log out and select "KDE on X11" at the login screen.

4. Set up the virtual display manually:
   ```bash
   sudo ./fedora-app/virtual-display.sh
   ```

## Android Setup (One-time)

1. Install the APK (either via Android Studio or sideloading):
   - Build from source using Android Studio, or
   - Copy the pre-built APK to your tablet and install it

2. Enable developer options and USB debugging on your tablet:
   - Go to Settings > About tablet
   - Tap Build number 7 times
   - Go back to Settings > System > Developer options
   - Enable USB debugging

## Connection (Each Time)

1. Connect your tablet to your Fedora computer via USB-C

2. Enable USB tethering on your tablet:
   - Go to Settings > Network & Internet > Hotspot & tethering
   - Enable "USB tethering"

3. Start the streaming and input handler services:
   ```bash
   # In one terminal
   ./fedora-app/stream-display.sh VIRTUAL1

   # In another terminal
   sudo python3 ./fedora-app/input-handler.py --display VIRTUAL1
   ```

4. Open the AndroidDisplay app on your tablet

5. Enter your Fedora system's IP address:
   - Typically 192.168.42.1 when using USB tethering
   - You can check with `ip addr show`

6. Tap "Connect" in the app

7. Your tablet should now display the content of your virtual display!

## Troubleshooting

- If the virtual display isn't recognized, try restarting your X session
- If the app can't connect, check your IP address and make sure USB tethering is enabled
- If video quality is poor, try:
  ```bash
  ./fedora-app/stream-display.sh VIRTUAL1 --quality=high
  ```
- For more details, see the full documentation in the docs/ directory