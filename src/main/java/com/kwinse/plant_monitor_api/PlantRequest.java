package com.kwinse.plant_monitor_api;

import java.time.LocalDate;

public record PlantRequest(String name, String species, LocalDate dateAcquired,
                           Double targetMoistureMin, Double targetMoistureMax) {
}
