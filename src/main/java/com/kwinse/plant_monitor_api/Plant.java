package com.kwinse.plant_monitor_api;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDate;

// One real plant. Things that change at repot time (pot size, soil, root condition) are not columns here,
// because overwriting them would lose the "before" (they belong in plant events later).
@Entity
public class Plant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String species;

    private LocalDate dateAcquired;

    // Optional until there's enough data to know what this plant likes.
    private Double targetMoistureMin;

    private Double targetMoistureMax;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public LocalDate getDateAcquired() {
        return dateAcquired;
    }

    public void setDateAcquired(LocalDate dateAcquired) {
        this.dateAcquired = dateAcquired;
    }

    public Double getTargetMoistureMin() {
        return targetMoistureMin;
    }

    public void setTargetMoistureMin(Double targetMoistureMin) {
        this.targetMoistureMin = targetMoistureMin;
    }

    public Double getTargetMoistureMax() {
        return targetMoistureMax;
    }

    public void setTargetMoistureMax(Double targetMoistureMax) {
        this.targetMoistureMax = targetMoistureMax;
    }
}
