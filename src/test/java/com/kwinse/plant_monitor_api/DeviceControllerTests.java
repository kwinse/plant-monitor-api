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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceController.class)
@TestPropertySource(properties = "API_KEY=test-key")
class DeviceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceRepository deviceRepository;

    @BeforeEach
    void setUp() {
        when(deviceRepository.existsByLabel("NODE-01")).thenReturn(true);
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ResultActions postDevice(String json) throws Exception {
        return mockMvc.perform(post("/api/devices")
                .header("X-API-KEY", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void validDeviceIsSaved() throws Exception {
        postDevice("""
                {"label": "NODE-02", "dryRaw": 2900, "wetRaw": 900}
                """).andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("NODE-02"));
    }

    @Test
    void labelIsTrimmed() throws Exception {
        postDevice("""
                {"label": "  NODE-02 ", "dryRaw": 2900, "wetRaw": 900}
                """).andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("NODE-02"));
    }

    @Test
    void blankLabelIsRejected() throws Exception {
        postDevice("""
                {"label": "  ", "dryRaw": 2900, "wetRaw": 900}
                """).andExpect(status().isBadRequest());
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void missingCalibrationIsRejected() throws Exception {
        postDevice("""
                {"label": "NODE-02", "dryRaw": 2900}
                """).andExpect(status().isBadRequest());
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void calibrationOutOfAdcRangeIsRejected() throws Exception {
        // The wet_raw = 9000 typo: would make every % from this device wrong
        postDevice("""
                {"label": "NODE-02", "dryRaw": 2900, "wetRaw": 9000}
                """).andExpect(status().isBadRequest());
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void swappedDryAndWetIsRejected() throws Exception {
        postDevice("""
                {"label": "NODE-02", "dryRaw": 900, "wetRaw": 2900}
                """).andExpect(status().isBadRequest());
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void equalDryAndWetIsRejected() throws Exception {
        postDevice("""
                {"label": "NODE-02", "dryRaw": 1500, "wetRaw": 1500}
                """).andExpect(status().isBadRequest());
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void duplicateLabelIsConflict() throws Exception {
        postDevice("""
                {"label": "NODE-01", "dryRaw": 2900, "wetRaw": 900}
                """).andExpect(status().isConflict());
        verify(deviceRepository, never()).save(any());
    }
}
