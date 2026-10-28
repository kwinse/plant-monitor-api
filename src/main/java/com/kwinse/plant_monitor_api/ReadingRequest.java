package com.kwinse.plant_monitor_api;

// What a device is allowed to send. The server fills in everything else (plant, moisture %, timestamp),
// so a client can't set those directly.
public record ReadingRequest(Long deviceId, Integer moistureRaw, Double light, Integer rssi) {
}
