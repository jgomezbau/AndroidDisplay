#!/bin/bash
# Script to create a virtual display for streaming to Android tablet

# Check if running as root
if [ "$(id -u)" -ne 0 ]; then
  echo "This script must be run as root" >&2
  exit 1
fi

# Detect primary display
PRIMARY_DISPLAY=$(xrandr | grep " connected primary" | cut -d" " -f1)
if [ -z "$PRIMARY_DISPLAY" ]; then
  PRIMARY_DISPLAY=$(xrandr | grep " connected" | head -n1 | cut -d" " -f1)
fi

echo "Primary display detected: $PRIMARY_DISPLAY"

# Find a virtual output that's available
VIRTUAL_OUTPUT=""
for i in {1..10}; do
  if xrandr | grep -q "VIRTUAL$i disconnected"; then
    VIRTUAL_OUTPUT="VIRTUAL$i"
    break
  fi
done

if [ -z "$VIRTUAL_OUTPUT" ]; then
  echo "No virtual output available. Make sure xf86-video-dummy is installed."
  exit 1
fi

echo "Using virtual output: $VIRTUAL_OUTPUT"

# Default resolution for Lenovo P11 (adjust as needed)
RESOLUTION="1600x1200"
REFRESH_RATE="60.00"

# Create new mode if it doesn't exist
if ! xrandr | grep -q "${RESOLUTION}_${REFRESH_RATE}"; then
  echo "Creating new mode: ${RESOLUTION}_${REFRESH_RATE}"
  xrandr --newmode "${RESOLUTION}_${REFRESH_RATE}" 161.00 1600 1712 1880 2160 1200 1203 1207 1245 -hsync +vsync
fi

# Add mode to virtual output
echo "Adding mode to virtual output"
xrandr --addmode "$VIRTUAL_OUTPUT" "${RESOLUTION}_${REFRESH_RATE}"

# Position to the right of primary display (can be changed to --left-of, --above, --below)
echo "Enabling virtual display"
xrandr --output "$VIRTUAL_OUTPUT" --right-of "$PRIMARY_DISPLAY" --mode "${RESOLUTION}_${REFRESH_RATE}"

echo "Virtual display $VIRTUAL_OUTPUT has been set up successfully at $RESOLUTION"