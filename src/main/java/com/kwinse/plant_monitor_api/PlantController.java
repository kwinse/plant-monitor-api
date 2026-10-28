package com.kwinse.plant_monitor_api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
public class PlantController {

    private final PlantRepository plantRepository;

    public PlantController(PlantRepository plantRepository) {
        this.plantRepository = plantRepository;
    }

    @GetMapping("/api/plants")
    public List<Plant> getAllPlants() {
        return plantRepository.findAll();
    }

    @PostMapping("/api/plants")
    public ResponseEntity<?> addPlant(@RequestBody PlantRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().body("Missing name.");
        }
        if (request.dateAcquired() != null && request.dateAcquired().isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest().body("Invalid dateAcquired: can't be in the future.");
        }
        if (isOutOfPercentRange(request.targetMoistureMin()) || isOutOfPercentRange(request.targetMoistureMax())) {
            return ResponseEntity.badRequest().body("Invalid target moisture: must be 0-100.");
        }
        if (request.targetMoistureMin() != null && request.targetMoistureMax() != null
                && request.targetMoistureMin() > request.targetMoistureMax()) {
            return ResponseEntity.badRequest().body("Invalid target moisture: min is greater than max.");
        }

        Plant plant = new Plant();
        plant.setName(request.name().trim());
        plant.setSpecies(request.species());
        plant.setDateAcquired(request.dateAcquired());
        plant.setTargetMoistureMin(request.targetMoistureMin());
        plant.setTargetMoistureMax(request.targetMoistureMax());
        return ResponseEntity.ok(plantRepository.save(plant));
    }

    private boolean isOutOfPercentRange(Double value) {
        return value != null && (value < 0 || value > 100);
    }
}
