import requests, random, time, os
from dotenv import load_dotenv

load_dotenv()

url = "http://localhost:8080/api/readings"
api_key = os.getenv("API_KEY")
if api_key is None:
    raise RuntimeError(f"API_KEY not found.")
headers = {"X-API-KEY": api_key}

def send_reading():
    # Same shape the ESP32 sends. Raw range is node 1's calibration (900 wet, 2900 dry).
    payload = {
        "deviceId": 1,
        "moistureRaw": random.randint(900, 2900),
        "light": round(random.uniform(0, 1000), 1),
        "rssi": random.randint(-80, -40),
    }
    response = requests.post(url, json=payload, headers=headers)
    if response.status_code != 200:
        print(f"Rejected: {response.status_code} - {response.text}")

def get_reading():
    response = requests.get(url + "/latest", headers=headers)
    print(f"Latest: {response.status_code} - {response.text}")
    
def tick():
    while True:
        send_reading()
        get_reading()
        time.sleep(5)
    
tick()