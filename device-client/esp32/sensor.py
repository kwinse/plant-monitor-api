from machine import ADC, Pin

def connect_sensor():
    sensor = ADC(Pin(34))
    sensor.atten(ADC.ATTN_11DB)
    return sensor

def read_moisture():
    sensor = connect_sensor()
    raw = sensor.read()
    moisture = 100 - (raw / 4095*100)
    return round(moisture, 2)