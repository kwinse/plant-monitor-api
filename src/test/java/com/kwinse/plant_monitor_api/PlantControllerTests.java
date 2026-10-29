package com.kwinse.plant_monitor_api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlantController.class)
@TestPropertySource(properties = "API_KEY=test-key")
class PlantControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlantRepository plantRepository;

    @BeforeEach
    void setUp() {
        when(plantRepository.save(any(Plant.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ResultActions postPlant(String json) throws Exception {
        return mockMvc.perform(post("/api/plants")
                .header("X-API-KEY", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void nameOnlyIsEnough() throws Exception {
        postPlant("""
                {"name": "Pink Princess"}
                """).andExpect(status().isOk());
    }

    @Test
    void fullPlantIsSaved() throws Exception {
        postPlant("""
                {"name": "Pink Princess", "species": "Philodendron erubescens", "dateAcquired": "2025-06-01",
                 "targetMoistureMin": 30, "targetMoistureMax": 60}
                """).andExpect(status().isOk());
    }

    @Test
    void missingNameIsRejected() throws Exception {
        postPlant("""
                {"species": "Monstera deliciosa"}
                """).andExpect(status().isBadRequest());
        verify(plantRepository, never()).save(any());
    }

    @Test
    void futureDateAcquiredIsRejected() throws Exception {
        postPlant("""
                {"name": "Pink Princess", "dateAcquired": "%s"}
                """.formatted(LocalDate.now().plusDays(1))).andExpect(status().isBadRequest());
        verify(plantRepository, never()).save(any());
    }

    @Test
    void targetAboveHundredIsRejected() throws Exception {
        postPlant("""
                {"name": "Pink Princess", "targetMoistureMax": 101}
                """).andExpect(status().isBadRequest());
        verify(plantRepository, never()).save(any());
    }

    @Test
    void targetMinAboveMaxIsRejected() throws Exception {
        postPlant("""
                {"name": "Pink Princess", "targetMoistureMin": 60, "targetMoistureMax": 30}
                """).andExpect(status().isBadRequest());
        verify(plantRepository, never()).save(any());
    }
}
