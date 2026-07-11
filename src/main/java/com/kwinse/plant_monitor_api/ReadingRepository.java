package com.kwinse.plant_monitor_api;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReadingRepository extends JpaRepository<Reading, Long> {
    List<Reading> findByMoisture(Double moisture);

    List<Reading> findByMoistureAndTimestampBetween(Double moisture, LocalDateTime start, LocalDateTime end);
}
