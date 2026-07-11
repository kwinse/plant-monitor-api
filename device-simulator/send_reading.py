import requests
import random
import time

url = "http://localhost:8080/api/readings"

def send_reading():
    mock_moisture = random.uniform(0, 100)
    payload = {"moisture": mock_moisture}
    response = requests.post(url, json=payload)
    print(f"Sent: {payload}")
    print(f"Response: {response.status_code} - {response.text}")
    
def tick():
    while True:
        send_reading()
        time.sleep(5)
    
tick()