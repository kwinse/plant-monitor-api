package com.kwinse.plant_monitor_api;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    boolean existsByLabel(String label);
}
