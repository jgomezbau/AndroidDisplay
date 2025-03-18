#!/bin/bash
# Script to stream the virtual display to Android tablet

# Configuration
VIRTUAL_DISPLAY=$1
USB_IP="192.168.42.129" # Default Android USB tethering IP
WIFI_IP=""
PORT=5000
USE_HARDWARE_ACCEL=true
STREAM_QUALITY="medium" # low, medium, high
FPS=30

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --display=*)
      VIRTUAL_DISPLAY="${1#*=}"
      ;;
    --usb-ip=*)
      USB_IP="${1#*=}"
      ;;
    --wifi-ip=*)
      WIFI_IP="${1#*=}"
      ;;
    --port=*)
      PORT="${1#*=}"
      ;;
    --quality=*)
      STREAM_QUALITY="${1#*=}"
      ;;
    --fps=*)
      FPS="${1#*=}"
      ;;
    --no-hw-accel)
      USE_HARDWARE_ACCEL=false
      ;;
    *)
      # Unknown option
      ;;
  esac
  shift
done

if [ -z "$VIRTUAL_DISPLAY" ]; then
  echo "Error: Virtual display not specified"
  echo "Usage: $0 VIRTUAL_DISPLAY [--usb-ip=IP] [--wifi-ip=IP] [--port=PORT] [--quality=QUALITY] [--fps=FPS]"
  exit 1
fi

# Get display resolution
RESOLUTION=$(xrandr | grep "$VIRTUAL_DISPLAY" | grep -o '[0-9]*x[0-9]*' | head -n1)
if [ -z "$RESOLUTION" ]; then
  echo "Error: Could not determine resolution of $VIRTUAL_DISPLAY"
  exit 1
fi

echo "Streaming display $VIRTUAL_DISPLAY at resolution $RESOLUTION"

# Set encoding parameters based on quality
case $STREAM_QUALITY in
  low)
    BITRATE="1500k"
    PRESET="ultrafast"
    ;;
  medium)
    BITRATE="3000k"
    PRESET="veryfast"
    ;;
  high)
    BITRATE="5000k"
    PRESET="fast"
    ;;
  *)
    BITRATE="3000k"
    PRESET="veryfast"
    ;;
esac

# Check connection availability - prefer USB
TARGET_IP=$USB_IP
if ! ping -c 1 -W 1 $USB_IP > /dev/null 2>&1; then
  if [ -n "$WIFI_IP" ] && ping -c 1 -W 1 $WIFI_IP > /dev/null 2>&1; then
    echo "USB connection not available, using Wi-Fi: $WIFI_IP"
    TARGET_IP=$WIFI_IP
  else
    echo "Error: Neither USB nor Wi-Fi connection is available"
    exit 1
  fi
fi

# Determine if hardware acceleration is available
HW_ACCEL_OPTS=""
if [ "$USE_HARDWARE_ACCEL" = true ]; then
  if vainfo > /dev/null 2>&1; then
    if vainfo | grep -q "H264"; then
      echo "Using VA-API hardware acceleration for H.264 encoding"
      HW_ACCEL_OPTS="-vaapi_device /dev/dri/renderD128 -vf 'format=nv12,hwupload' -c:v h264_vaapi"
    else
      echo "H.264 encoding not supported by VA-API, falling back to software encoding"
    fi
  else
    echo "VA-API not available, falling back to software encoding"
  fi
fi

# If hardware acceleration isn't available or enabled, use software encoding
if [ -z "$HW_ACCEL_OPTS" ]; then
  echo "Using software H.264 encoding"
  HW_ACCEL_OPTS="-c:v libx264 -preset $PRESET -tune zerolatency"
fi

# Start streaming
echo "Streaming to $TARGET_IP:$PORT"
ffmpeg -f x11grab -r $FPS -s $RESOLUTION -i :0.0+$VIRTUAL_DISPLAY \
  $HW_ACCEL_OPTS -b:v $BITRATE -maxrate $BITRATE -bufsize $(($BITRATE/2)) \
  -pix_fmt yuv420p -g $(($FPS*2)) -keyint_min $FPS \
  -f mpegts udp://$TARGET_IP:$PORT