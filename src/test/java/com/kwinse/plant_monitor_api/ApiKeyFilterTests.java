package com.kwinse.plant_monitor_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReadingController.class)
@TestPropertySource(properties = "API_KEY=test-key")
class ApiKeyFilterTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReadingRepository readingRepository;

    @Test
    void missingKeyIsRejected() throws Exception {
        mockMvc.perform(get("/api/readings/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongKeyIsRejected() throws Exception {
        mockMvc.perform(get("/api/readings/all").header("X-API-KEY", "wrong-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void correctKeyIsAccepted() throws Exception {
        mockMvc.perform(get("/api/readings/all").header("X-API-KEY", "test-key"))
                .andExpect(status().isOk());
    }
}
