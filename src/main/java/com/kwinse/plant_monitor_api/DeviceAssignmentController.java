package com.kwinse.plant_monitor_api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
public class DeviceAssignmentController {

    private final DeviceAssignmentRepository assignmentRepository;
    private final DeviceRepository deviceRepository;
    private final PlantRepository plantRepository;

    public DeviceAssignmentController(DeviceAssignmentRepository assignmentRepository,
                                      DeviceRepository deviceRepository,
                                      PlantRepository plantRepository) {
        this.assignmentRepository = assignmentRepository;
        this.deviceRepository = deviceRepository;
        this.plantRepository = plantRepository;
    }

    @GetMapping("/api/assignments")
    public List<DeviceAssignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    // Moves a device to a plant: ends its current assignment (if any) and starts a new one.
    // @Transactional so both happen or neither does; a crash in between can't leave the device with no plant or two.
    @PostMapping("/api/assignments")
    @Transactional
    public ResponseEntity<?> assignDevice(@RequestBody AssignmentRequest request) {
        if (request.deviceId() == null || request.plantId() == null) {
            return ResponseEntity.badRequest().body("Missing deviceId or plantId.");
        }
        Optional<Device> device = deviceRepository.findById(request.deviceId());
        if (device.isEmpty()) {
            return ResponseEntity.badRequest().body("Unknown deviceId.");
        }
        Optional<Plant> plant = plantRepository.findById(request.plantId());
        if (plant.isEmpty()) {
            return ResponseEntity.badRequest().body("Unknown plantId.");
        }

        LocalDateTime now = LocalDateTime.now().withNano(0);
        Optional<DeviceAssignment> current = assignmentRepository.findByDeviceIdAndEndTimeIsNull(request.deviceId());
        if (current.isPresent()) {
            if (current.get().getPlant().getId().equals(request.plantId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Device is already assigned to this plant.");
            }
            current.get().setEndTime(now);
            assignmentRepository.save(current.get());
        }

        DeviceAssignment assignment = new DeviceAssignment();
        assignment.setDevice(device.get());
        assignment.setPlant(plant.get());
        assignment.setStartTime(now);
        return ResponseEntity.ok(assignmentRepository.save(assignment));
    }
}
