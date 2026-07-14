import network, time, urequests
from secrets import WIFI_SSID, WIFI_PASSWORD, API_URL, API_KEY
from sensor import read_moisture

def connect_wifi():
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    if not wlan.isconnected():
        print("Connecting to WiFi...")
        wlan.connect(WIFI_SSID, WIFI_PASSWORD)
        while not wlan.isconnected():
            time.sleep(0.5)
        print("Connected! IP:", wlan.ifconfig()[0])
        
def send_reading():
    moisture = read_moisture()
    headers = {"X-API-KEY": API_KEY, "Content-Type": "application/json"}    
    payload = {"moisture": moisture}
    try:
        response = urequests.post(API_URL, json=payload, headers=headers)
        print("Sent:", payload, "Response:", response.status_code)
        response.close()
    except Exception as e:
        print("Failure to send:", e)

connect_wifi()

while True:
    send_reading()
    time.sleep(5)