package com.kwinse.plant_monitor_api;

public record DeviceRequest(String label, Integer dryRaw, Integer wetRaw) {
}
