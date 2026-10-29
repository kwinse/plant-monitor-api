package com.kwinse.plant_monitor_api;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceAssignmentRepository extends JpaRepository<DeviceAssignment, Long> {
    // The device's current assignment (the one that hasn't ended yet), if any.
    Optional<DeviceAssignment> findByDeviceIdAndEndTimeIsNull(Long deviceId);
}
