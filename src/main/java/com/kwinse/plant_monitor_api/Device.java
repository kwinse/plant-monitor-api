package com.kwinse.plant_monitor_api;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

// One physical ESP32 node. Its label is what's written on the board, and it never changes when the node moves to another plant.
@Entity
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String label;

    // Calibration: raw ADC value in air (driest) and in water (wettest). Belongs to the sensor, not the plant.
    @Column(nullable = false)
    private Integer dryRaw;

    @Column(nullable = false)
    private Integer wetRaw;

    // Capacitive sensors read lower when wetter, so dryRaw maps to 0% and wetRaw to 100%.
    // Clamped because raw values can land slightly outside the calibration range; the raw value is still stored as-is.
    public double moisturePercent(int raw) {
        double percent = (double) (dryRaw - raw) / (dryRaw - wetRaw) * 100;
        percent = Math.max(0, Math.min(100, percent));
        return Math.round(percent * 10) / 10.0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getDryRaw() {
        return dryRaw;
    }

    public void setDryRaw(Integer dryRaw) {
        this.dryRaw = dryRaw;
    }

    public Integer getWetRaw() {
        return wetRaw;
    }

    public void setWetRaw(Integer wetRaw) {
        this.wetRaw = wetRaw;
    }
}
