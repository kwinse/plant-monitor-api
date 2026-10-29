package com.kwinse.plant_monitor_api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
public class ReadingController {

    private final ReadingRepository readingRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceAssignmentRepository assignmentRepository;

    public ReadingController(ReadingRepository readingRepository,
                             DeviceRepository deviceRepository,
                             DeviceAssignmentRepository assignmentRepository) {
        this.readingRepository = readingRepository;
        this.deviceRepository = deviceRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @GetMapping("/api/readings/all")
    public List<Reading> getAllReadings() {
        return readingRepository.findAll();
    }

    @GetMapping("/api/readings/latest")
    public ResponseEntity<Reading> getLatestReading() {
        Reading latest = readingRepository.findTopByOrderByTimestampDesc();
        if (latest == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(latest);
    }

    @GetMapping("/api/readings/{id}")
    public Reading getReading(@PathVariable Long id) {
        return readingRepository.findById(id).orElseThrow();
    }

    @GetMapping("/api/readings/by-moisture/{moisture}")
    public List<Reading> getReadingsByMoisture(@PathVariable Double moisture) {
        return readingRepository.findByMoisture(moisture);
    }

    @GetMapping("/api/readings/by-moisture-and-timestamp")
    public List<Reading> getReadingsByMoistureAndTimestampBetween(
            @RequestParam Double moisture,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return readingRepository.findByMoistureAndTimestampBetween(moisture, start, end);
    }

    @PostMapping("/api/readings")
    public ResponseEntity<?> receiveReading(@RequestBody ReadingRequest request) {
        if (request.deviceId() == null) {
            return ResponseEntity.badRequest().body("Missing deviceId.");
        }
        if (request.moistureRaw() == null) {
            return ResponseEntity.badRequest().body("Missing moistureRaw.");
        }
        // ESP32 ADC is 12-bit
        if (request.moistureRaw() < 0 || request.moistureRaw() > 4095) {
            return ResponseEntity.badRequest().body("Invalid moistureRaw: must be 0-4095.");
        }
        // BH1750 range. Null is allowed: it means the light read failed.
        if (request.light() != null && (request.light() < 0 || request.light() > 65535)) {
            return ResponseEntity.badRequest().body("Invalid light: must be 0-65535 lux, or null.");
        }
        if (request.rssi() != null && (request.rssi() < -120 || request.rssi() > 0)) {
            return ResponseEntity.badRequest().body("Invalid rssi: must be -120 to 0 dBm, or null.");
        }

        Optional<Device> device = deviceRepository.findById(request.deviceId());
        if (device.isEmpty()) {
            return ResponseEntity.badRequest().body("Unknown deviceId.");
        }

        // Stamp the plant the device is in right now. No open assignment = save with plant null rather than lose the reading.
        Plant plant = assignmentRepository.findByDeviceIdAndEndTimeIsNull(request.deviceId())
                .map(DeviceAssignment::getPlant)
                .orElse(null);

        Reading reading = new Reading();
        reading.setDevice(device.get());
        reading.setPlant(plant);
        reading.setMoistureRaw(request.moistureRaw());
        reading.setMoisture(device.get().moisturePercent(request.moistureRaw()));
        reading.setLight(request.light());
        reading.setRssi(request.rssi());
        reading.setTimestamp(LocalDateTime.now().withNano(0));
        return ResponseEntity.ok(readingRepository.save(reading));
    }
}
