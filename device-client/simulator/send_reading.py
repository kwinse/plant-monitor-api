import requests, random, time, os
from dotenv import load_dotenv

load_dotenv()

url = "http://localhost:8080/api/readings"
api_key = os.getenv("API_KEY")
if api_key is None:
    raise RuntimeError(f"API_KEY not found.")
headers = {"X-API-KEY": api_key}

def send_reading():
    mock_moisture = random.uniform(0, 100)
    payload = {"moisture": mock_moisture}
    response = requests.post(url, json=payload, headers=headers)

def get_reading():
    response = requests.get(url + "/latest", headers=headers)
    print(f"Latest: {response.status_code} - {response.text}")
    
def tick():
    while True:
        send_reading()
        get_reading()
        time.sleep(5)
    
tick()