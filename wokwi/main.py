"""
MicroPython IoT Weather Station Example for Wokwi.com

To view the data:

1. Go to http://www.hivemq.com/demos/websocket-client/
2. Click "Connect"
3. Under Subscriptions, click "Add New Topic Subscription"
4. In the Topic field, type "bme280-weather" then click "Subscribe"

Now click on the BME280 sensor in the simulation,
change the temperature/humidity, and you should see
the message appear on the MQTT Broker, in the "Messages" pane.

https://wokwi.com/projects/452313079944044545
"""

import network
from machine import Pin, I2C
from time import sleep
import ujson
from umqtt.simple import MQTTClient

# --- I2C setup ---
# Address 0x76 must match the C code
BME_ADDR = 0x76
i2c = I2C(0, scl=Pin(22), sda=Pin(21), freq=100000)

def read_sensor_data():
    """Burst read 8 bytes starting from Pressure MSB (0xF7)"""
    # 0xF7 is the first register for Pressure, Temp, and Hum data
    data = i2c.readfrom_mem(BME_ADDR, 0xF7, 8)
    
    # Pressure (Bytes 0, 1, 2)
    press = (data[0] << 12) | (data[1] << 4) | (data[2] >> 4)
    
    # Temperature (Bytes 3, 4, 5)
    raw_t = (data[3] << 12) | (data[4] << 4) | (data[5] >> 4)
    # Handle negative values (20-bit sign extension)
    if raw_t & 0x80000:
        raw_t -= 0x100000
    temp = raw_t
    
    # Humidity (Bytes 6, 7)
    hum = (data[6] << 8) | data[7]
    
    return temp, press, hum

# --- WiFi + MQTT Setup ---
SSID = 'Wokwi-GUEST'
PASS = ''
MQTT_CLIENT_ID = 'esp32-balcony-sensor'
MQTT_BROKER    = 'broker.mqttdashboard.com'
MQTT_TOPIC     = 'bme280-weather'

print("Connecting to WiFi...", end="")
sta = network.WLAN(network.STA_IF)
sta.active(True)
sta.connect(SSID, PASS)
while not sta.isconnected():
    print(".", end="")
    sleep(0.1)
print(" Connected!")

print("Connecting to MQTT Broker...", end="")
client = MQTTClient(MQTT_CLIENT_ID, MQTT_BROKER)
client.connect()
print(" Connected!")

# --- Main Loop ---
prev_payload = ""

while True:
    try:
        t, p, h = read_sensor_data()
        
        # Prepare integer-only JSON
        payload = ujson.dumps({
            "temp": t,
            "pressure": p,
            "humidity": h
        })
        
        # Publish only if sliders moved
        if payload != prev_payload:
            print("Publishing to MQTT:", payload)
            client.publish(MQTT_TOPIC, payload)
            prev_payload = payload
            
    except Exception as e:
        print("Loop Error:", e)
        
    sleep(2)