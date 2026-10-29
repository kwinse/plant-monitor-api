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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceAssignmentController.class)
@TestPropertySource(properties = "API_KEY=test-key")
class DeviceAssignmentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceAssignmentRepository assignmentRepository;

    @MockitoBean
    private DeviceRepository deviceRepository;

    @MockitoBean
    private PlantRepository plantRepository;

    private Device node1;
    private Plant pinkPrincess;
    private Plant cutting;

    @BeforeEach
    void setUp() {
        node1 = new Device();
        node1.setId(1L);
        pinkPrincess = new Plant();
        pinkPrincess.setId(1L);
        cutting = new Plant();
        cutting.setId(2L);

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(node1));
        when(plantRepository.findById(1L)).thenReturn(Optional.of(pinkPrincess));
        when(plantRepository.findById(2L)).thenReturn(Optional.of(cutting));
        when(assignmentRepository.findByDeviceIdAndEndTimeIsNull(1L)).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(DeviceAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ResultActions postAssignment(String json) throws Exception {
        return mockMvc.perform(post("/api/assignments")
                .header("X-API-KEY", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private DeviceAssignment openAssignment(Plant plant) {
        DeviceAssignment assignment = new DeviceAssignment();
        assignment.setDevice(node1);
        assignment.setPlant(plant);
        assignment.setStartTime(LocalDateTime.now().minusDays(30));
        return assignment;
    }

    @Test
    void firstAssignmentIsOpen() throws Exception {
        postAssignment("""
                {"deviceId": 1, "plantId": 1}
                """).andExpect(status().isOk())
                .andExpect(jsonPath("$.endTime").doesNotExist());
    }

    @Test
    void movingDeviceEndsTheOldAssignment() throws Exception {
        DeviceAssignment old = openAssignment(pinkPrincess);
        when(assignmentRepository.findByDeviceIdAndEndTimeIsNull(1L)).thenReturn(Optional.of(old));

        postAssignment("""
                {"deviceId": 1, "plantId": 2}
                """).andExpect(status().isOk())
                .andExpect(jsonPath("$.plant.id").value(2));

        assertThat(old.getEndTime()).isNotNull();
    }

    @Test
    void assigningToSamePlantAgainIsConflict() throws Exception {
        DeviceAssignment old = openAssignment(pinkPrincess);
        when(assignmentRepository.findByDeviceIdAndEndTimeIsNull(1L)).thenReturn(Optional.of(old));

        postAssignment("""
                {"deviceId": 1, "plantId": 1}
                """).andExpect(status().isConflict());

        assertThat(old.getEndTime()).isNull();
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void missingIdsAreRejected() throws Exception {
        postAssignment("""
                {"deviceId": 1}
                """).andExpect(status().isBadRequest());
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void unknownDeviceIsRejected() throws Exception {
        postAssignment("""
                {"deviceId": 99, "plantId": 1}
                """).andExpect(status().isBadRequest());
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void unknownPlantIsRejected() throws Exception {
        postAssignment("""
                {"deviceId": 1, "plantId": 99}
                """).andExpect(status().isBadRequest());
        verify(assignmentRepository, never()).save(any());
    }
}
