package com.kwinse.plant_monitor_api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// For each field: missing, out of range, boundary, and weird-but-real. Invalid readings must never reach the database.
@WebMvcTest(ReadingController.class)
@TestPropertySource(properties = "API_KEY=test-key")
class ReadingValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReadingRepository readingRepository;

    @MockitoBean
    private DeviceRepository deviceRepository;

    @MockitoBean
    private DeviceAssignmentRepository assignmentRepository;

    private Plant pinkPrincess;

    @BeforeEach
    void setUp() {
        Device node1 = new Device();
        node1.setId(1L);
        node1.setLabel("NODE-01");
        node1.setDryRaw(2900);
        node1.setWetRaw(900);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(node1));
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        pinkPrincess = new Plant();
        pinkPrincess.setId(1L);
        pinkPrincess.setName("Pink Princess");
        DeviceAssignment assignment = new DeviceAssignment();
        assignment.setDevice(node1);
        assignment.setPlant(pinkPrincess);
        when(assignmentRepository.findByDeviceIdAndEndTimeIsNull(1L)).thenReturn(Optional.of(assignment));

        // save() returns whatever it was given, like the real repository would
        when(readingRepository.save(any(Reading.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ResultActions postReading(String json) throws Exception {
        return mockMvc.perform(post("/api/readings")
                .header("X-API-KEY", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private Reading savedReading() {
        ArgumentCaptor<Reading> captor = ArgumentCaptor.forClass(Reading.class);
        verify(readingRepository).save(captor.capture());
        return captor.getValue();
    }

    private void assertNothingSaved() {
        verify(readingRepository, never()).save(any());
    }

    // --- valid readings ---

    @Test
    void validReadingIsSavedWithPlantAndPercent() throws Exception {
        postReading("""
                {"deviceId": 1, "moistureRaw": 1900, "light": 381.5, "rssi": -60}
                """).andExpect(status().isOk());

        Reading saved = savedReading();
        assertThat(saved.getPlant()).isSameAs(pinkPrincess);
        assertThat(saved.getMoistureRaw()).isEqualTo(1900);
        assertThat(saved.getMoisture()).isEqualTo(50.0);
        assertThat(saved.getLight()).isEqualTo(381.5);
        assertThat(saved.getRssi()).isEqualTo(-60);
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void failedLightReadIsSavedAsNull() throws Exception {
        postReading("""
                {"deviceId": 1, "moistureRaw": 1900, "light": null, "rssi": -60}
                """).andExpect(status().isOk());

        assertThat(savedReading().getLight()).isNull();
    }

    @Test
    void unassignedDeviceReadingIsKeptWithNullPlant() throws Exception {
        when(assignmentRepository.findByDeviceIdAndEndTimeIsNull(1L)).thenReturn(Optional.empty());

        postReading("""
                {"deviceId": 1, "moistureRaw": 1900}
                """).andExpect(status().isOk());

        assertThat(savedReading().getPlant()).isNull();
    }

    @Test
    void clientCannotSetPlantPercentOrTimestamp() throws Exception {
        // Extra fields aren't part of ReadingRequest, so they're ignored. The server decides these.
        postReading("""
                {"deviceId": 1, "moistureRaw": 1900, "moisture": 0, "plant": {"id": 42}, "timestamp": "01-01-2000 00:00:00"}
                """).andExpect(status().isOk());

        Reading saved = savedReading();
        assertThat(saved.getMoisture()).isEqualTo(50.0);
        assertThat(saved.getPlant()).isSameAs(pinkPrincess);
        assertThat(saved.getTimestamp().getYear()).isNotEqualTo(2000);
    }

    // --- deviceId ---

    @Test
    void missingDeviceIdIsRejected() throws Exception {
        postReading("""
                {"moistureRaw": 1900}
                """).andExpect(status().isBadRequest());
        assertNothingSaved();
    }

    @Test
    void unknownDeviceIsRejected() throws Exception {
        postReading("""
                {"deviceId": 99, "moistureRaw": 1900}
                """).andExpect(status().isBadRequest());
        assertNothingSaved();
    }

    // --- moistureRaw ---

    @Test
    void missingMoistureRawIsRejected() throws Exception {
        postReading("""
                {"deviceId": 1}
                """).andExpect(status().isBadRequest());
        assertNothingSaved();
    }

    @Test
    void negativeMoistureRawIsRejected() throws Exception {
        postReading("""
                {"deviceId": 1, "moistureRaw": -1}
                """).andExpect(status().isBadRequest());
        assertNothingSaved();
    }

    @Test
    void moistureRawAboveTwelveBitsIsRejected() throws Exception {
        postReading("""
                {"deviceId": 1, "moistureRaw": 4096}
                """).andExpect(status().isBadRequest());
        assertNothingSaved();
    }

    @Test
    void moistureRawBoundariesAreAccepted() throws Exception {
        postReading("""
                {"deviceId": 1, "moistureRaw": 0}
                """).andExpect(status().isOk());
        postReading("""
                {"deviceId": 1, "moistureRaw": 4095}
                """).andExpect(status().isOk());
    }

    @Test
    void wrongTypeMoistureRawIsRejected() throws Exception {
        postReading("""
                {"deviceId": 1, "moistureRaw": "wet"}
                """).andExpect(status().isBadRequest());
        assertNothingSaved();
    }

    // --- light ---

    @Test
    void negativeLightIsRejected() throws Exception {
        postReading("""
                {"deviceId": 1, "moistureRaw": 1900, "light": -1}
                """).andExpect(status().isBadRequest());
        assertNothingSaved();
    }

    @Test
    void lightAboveSensorMaxIsRejected() throws Exception {
        postReading("""
                {"deviceId": 1, "moistureRaw": 1900, "light": 65536}
                """).andExpect(status().isBadRequest());
        assertNothingSaved();
    }

    @Test
    void zeroLightIsAccepted() throws Exception {
        // A dark room at night is a real reading, not a failure
        postReading("""
                {"deviceId": 1, "moistureRaw": 1900, "light": 0}
                """).andExpect(status().isOk());
    }

    // --- rssi ---

    @Test
    void positiveRssiIsRejected() throws Exception {
        postReading("""
                {"deviceId": 1, "moistureRaw": 1900, "rssi": 5}
                """).andExpect(status().isBadRequest());
        assertNothingSaved();
    }

    @Test
    void rssiBelowRangeIsRejected() throws Exception {
        postReading("""
                {"deviceId": 1, "moistureRaw": 1900, "rssi": -121}
                """).andExpect(status().isBadRequest());
        assertNothingSaved();
    }
}
