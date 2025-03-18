#!/bin/bash
# Setup script for Android Display - Fedora component

# Check if running as root
if [ "$(id -u)" -ne 0 ]; then
  echo "This script must be run as root" >&2
  exit 1
fi

# Install required packages
echo "Installing required packages..."
dnf install -y ffmpeg xrandr xdotool libva-utils python3 python3-pip

# Install Python requirements
echo "Installing Python requirements..."
pip3 install argparse

# Make scripts executable
echo "Making scripts executable..."
chmod +x virtual-display.sh
chmod +x stream-display.sh
chmod +x input-handler.py

# Install systemd service
echo "Installing systemd service..."
cp android-display.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable android-display.service

# Install udev rule
echo "Installing udev rule..."
cp 99-lenovo-p11.rules /etc/udev/rules.d/
udevadm control --reload-rules

# Create X11 configuration for virtual display
echo "Creating X11 configuration for virtual display..."
cat > /etc/X11/xorg.conf.d/20-virtual-display.conf << EOF
Section "Device"  
    Identifier "Virtual Display"
    Driver "dummy"
    VideoRam 256000
EndSection
EOF

echo "Setup complete!"
echo "To update the device vendor and product IDs in the udev rule:"
echo "1. Connect your Lenovo P11 tablet via USB-C"
echo "2. Run 'lsusb' to find the vendor and product IDs"
echo "3. Update the values in /etc/udev/rules.d/99-lenovo-p11.rules"
echo "4. Run 'udevadm control --reload-rules'"
echo ""
echo "To start the service manually, run: systemctl start android-display.service"