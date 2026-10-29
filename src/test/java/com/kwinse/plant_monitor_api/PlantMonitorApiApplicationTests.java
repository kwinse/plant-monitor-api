package com.kwinse.plant_monitor_api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = "API_KEY=test-key")
class PlantMonitorApiApplicationTests {

	@Container
	@ServiceConnection
	static MySQLContainer mysql = new MySQLContainer("mysql:8");

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	// Full path against real MySQL: register device + plant, assign, send a reading, and check plant_id got stamped.
	@Test
	void readingIsStampedWithAssignedPlant() throws Exception {
		Long deviceId = idOf(postJson("/api/devices", """
				{"label": "IT-NODE", "dryRaw": 2900, "wetRaw": 900}
				"""));
		Long plantId = idOf(postJson("/api/plants", """
				{"name": "Integration Test Plant"}
				"""));
		postJson("/api/assignments", """
				{"deviceId": %d, "plantId": %d}
				""".formatted(deviceId, plantId)).andExpect(status().isOk());

		postJson("/api/readings", """
				{"deviceId": %d, "moistureRaw": 1900, "light": 120.0, "rssi": -55}
				""".formatted(deviceId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.plant.id").value(plantId))
				.andExpect(jsonPath("$.moisture").value(50.0));
	}

	private ResultActions postJson(String url, String json) throws Exception {
		return mockMvc.perform(post(url)
				.header("X-API-KEY", "test-key")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private Long idOf(ResultActions result) throws Exception {
		String body = result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

}
