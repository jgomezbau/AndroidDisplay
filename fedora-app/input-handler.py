#!/usr/bin/env python3
"""
Input handler for Android Display
Receives touch events from Android and maps them to X11 input events
"""

import socket
import struct
import subprocess
import argparse
import json
import logging
import time
import sys
import os
from threading import Thread

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler()]
)
logger = logging.getLogger('input-handler')

class InputHandler:
    """Handler for touch input events from Android"""
    
    # Touch event types
    ACTION_DOWN = 0
    ACTION_UP = 1
    ACTION_MOVE = 2
    
    def __init__(self, display, port=5001, virtual_display_offset=None):
        self.display = display
        self.port = port
        self.running = False
        self.socket = None
        
        # Get virtual display information if not provided
        if virtual_display_offset is None:
            self.get_display_info()
        else:
            self.display_offset_x, self.display_offset_y = virtual_display_offset
            
        logger.info(f"Virtual display offset: ({self.display_offset_x}, {self.display_offset_y})")
        
    def get_display_info(self):
        """Get information about the virtual display position"""
        try:
            # Get display geometry using xrandr
            xrandr_output = subprocess.check_output(['xrandr', '--current'], 
                                                   universal_newlines=True)
            
            # Parse output to find the virtual display position
            display_info = {}
            current_display = None
            
            for line in xrandr_output.splitlines():
                if ' connected ' in line:
                    parts = line.split()
                    current_display = parts[0]
                    if current_display == self.display:
                        # Extract position if in format like "1920x1080+1920+0"
                        geometry = [p for p in parts if '+' in p][0]
                        if geometry:
                            plus_split = geometry.split('+')
                            if len(plus_split) >= 3:
                                self.display_offset_x = int(plus_split[1])
                                self.display_offset_y = int(plus_split[2])
                                return
            
            # If we get here, we couldn't find the display info
            logger.error(f"Could not find geometry for display {self.display}")
            self.display_offset_x, self.display_offset_y = 0, 0
            
        except Exception as e:
            logger.error(f"Error getting display info: {e}")
            self.display_offset_x, self.display_offset_y = 0, 0
    
    def start(self):
        """Start the input handler server"""
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        
        try:
            self.socket.bind(('0.0.0.0', self.port))
            self.socket.listen(1)
            self.running = True
            
            logger.info(f"Input handler listening on port {self.port}")
            
            while self.running:
                client, addr = self.socket.accept()
                logger.info(f"Connection from {addr}")
                
                client_thread = Thread(target=self.handle_client, args=(client,))
                client_thread.daemon = True
                client_thread.start()
                
        except Exception as e:
            logger.error(f"Server error: {e}")
        finally:
            self.stop()
    
    def handle_client(self, client):
        """Handle client connection and process touch events"""
        try:
            buffer = b''
            
            while self.running:
                data = client.recv(1024)
                if not data:
                    break
                
                buffer += data
                
                # Process complete messages in the buffer
                while len(buffer) >= 13:  # 13 bytes is our message size
                    # Extract header and event data
                    event_type, x, y = struct.unpack('!Bii', buffer[:9])
                    buffer = buffer[9:]
                    
                    # Process the touch event
                    self.process_touch_event(event_type, x, y)
                    
            logger.info("Client disconnected")
            
        except Exception as e:
            logger.error(f"Error handling client: {e}")
        finally:
            client.close()
    
    def process_touch_event(self, event_type, x, y):
        """Map Android touch events to X11 input events"""
        # Adjust coordinates based on virtual display offset
        adjusted_x = x + self.display_offset_x
        adjusted_y = y + self.display_offset_y
        
        try:
            if event_type == self.ACTION_DOWN:
                # Mouse button press
                subprocess.run(['xdotool', 'mousemove', str(adjusted_x), str(adjusted_y)])
                subprocess.run(['xdotool', 'mousedown', '1'])
                
            elif event_type == self.ACTION_UP:
                # Mouse button release
                subprocess.run(['xdotool', 'mouseup', '1'])
                
            elif event_type == self.ACTION_MOVE:
                # Mouse movement
                subprocess.run(['xdotool', 'mousemove', str(adjusted_x), str(adjusted_y)])
                
        except Exception as e:
            logger.error(f"Error executing input command: {e}")
    
    def stop(self):
        """Stop the input handler server"""
        self.running = False
        if self.socket:
            self.socket.close()
            self.socket = None

def main():
    """Main function to start the input handler"""
    parser = argparse.ArgumentParser(description='Android Display Input Handler')
    parser.add_argument('--display', required=True, help='Name of the virtual display (e.g., VIRTUAL1)')
    parser.add_argument('--port', type=int, default=5001, help='Port to listen on for input events')
    parser.add_argument('--offset', type=str, help='Display offset in format "x,y"')
    args = parser.parse_args()
    
    # Parse display offset if provided
    display_offset = None
    if args.offset:
        try:
            x, y = args.offset.split(',')
            display_offset = (int(x), int(y))
        except Exception as e:
            logger.error(f"Invalid offset format: {e}")
            return 1
    
    # Create and start the input handler
    handler = InputHandler(args.display, args.port, display_offset)
    try:
        handler.start()
    except KeyboardInterrupt:
        logger.info("Stopping input handler")
        handler.stop()
    
    return 0

if __name__ == '__main__':
    sys.exit(main())