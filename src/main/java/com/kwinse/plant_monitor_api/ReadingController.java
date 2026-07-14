package com.kwinse.plant_monitor_api;

import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class ReadingController {

    private final ReadingRepository readingRepository;

    public ReadingController(ReadingRepository readingRepository) {
        this.readingRepository = readingRepository;
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
    public ResponseEntity<?> receiveReading(@RequestBody Reading reading) {
        if (reading.getMoisture() == null) {
            return ResponseEntity.badRequest().body("Missing moisture value.");
        }
        else if (reading.getMoisture() < 0 || reading.getMoisture() > 100) {
            return ResponseEntity.badRequest().body("Invalid moisture value.");
        }
        reading.setTimestamp(LocalDateTime.now().withNano(0));
        return ResponseEntity.ok(readingRepository.save(reading));
    }
}
