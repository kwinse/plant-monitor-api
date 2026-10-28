package com.kwinse.plant_monitor_api;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

// Indexes match how readings get queried: one plant's (or one device's) readings over a time range.
@Entity
@Table(indexes = {
        @Index(name = "idx_reading_plant_time", columnList = "plant_id, timestamp"),
        @Index(name = "idx_reading_device_time", columnList = "device_id, timestamp")
})
public class Reading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    // Stamped from the device's open assignment when the reading arrives. Null = device wasn't assigned to a plant yet.
    @ManyToOne
    @JoinColumn(name = "plant_id")
    private Plant plant;

    // What the sensor actually reported. Kept so moisture % can be recomputed if calibration changes.
    @Column(nullable = false)
    private Integer moistureRaw;

    // Percent, computed once from the device's calibration.
    @Column(nullable = false)
    private Double moisture;

    // Lux. Null = the light sensor read failed (never a fake value).
    private Double light;

    // WiFi signal strength in dBm, for spotting a node with a weak connection.
    private Integer rssi;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Plant getPlant() {
        return plant;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
    }

    public Integer getMoistureRaw() {
        return moistureRaw;
    }

    public void setMoistureRaw(Integer moistureRaw) {
        this.moistureRaw = moistureRaw;
    }

    public Double getMoisture() {
        return moisture;
    }

    public void setMoisture(Double moisture) {
        this.moisture = moisture;
    }

    public Double getLight() {
        return light;
    }

    public void setLight(Double light) {
        this.light = light;
    }

    public Integer getRssi() {
        return rssi;
    }

    public void setRssi(Integer rssi) {
        this.rssi = rssi;
    }

    @JsonFormat(pattern = "MM-dd-yyyy HH:mm:ss")
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
