package com.kwinse.plant_monitor_api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DeviceController {

    private final DeviceRepository deviceRepository;

    public DeviceController(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @GetMapping("/api/devices")
    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    @PostMapping("/api/devices")
    public ResponseEntity<?> registerDevice(@RequestBody DeviceRequest request) {
        if (request.label() == null || request.label().isBlank()) {
            return ResponseEntity.badRequest().body("Missing label.");
        }
        if (request.dryRaw() == null || request.wetRaw() == null) {
            return ResponseEntity.badRequest().body("Missing dryRaw or wetRaw.");
        }
        if (request.dryRaw() < 0 || request.dryRaw() > 4095 || request.wetRaw() < 0 || request.wetRaw() > 4095) {
            return ResponseEntity.badRequest().body("Invalid calibration: dryRaw and wetRaw must be 0-4095.");
        }
        // Capacitive sensors read lower when wet. Equal values would also divide by zero in moisturePercent.
        if (request.dryRaw() <= request.wetRaw()) {
            return ResponseEntity.badRequest().body("Invalid calibration: dryRaw must be greater than wetRaw.");
        }
        String label = request.label().trim();
        if (deviceRepository.existsByLabel(label)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("A device with this label already exists.");
        }

        Device device = new Device();
        device.setLabel(label);
        device.setDryRaw(request.dryRaw());
        device.setWetRaw(request.wetRaw());
        return ResponseEntity.ok(deviceRepository.save(device));
    }
}
