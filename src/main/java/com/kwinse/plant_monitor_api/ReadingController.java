package com.kwinse.plant_monitor_api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
public class ReadingController {

    private final ReadingRepository readingRepository;

    public ReadingController(ReadingRepository readingRepository) {
        this.readingRepository = readingRepository;
    }

    @GetMapping("/api/readings/{id}")
    public Reading getReading(@PathVariable Long id) {
        return readingRepository.findById(id).orElseThrow();
    }

    @PostMapping("/api/readings")
    public ResponseEntity<?> receiveReading(@RequestBody Reading reading) {
        if (reading.getMoisture() < 0 || reading.getMoisture() > 100) {
            return ResponseEntity.badRequest().body("Invalid moisture value.");
        }
        reading.setTimestamp(LocalDateTime.now());
        return ResponseEntity.ok(readingRepository.save(reading));
    }
}
