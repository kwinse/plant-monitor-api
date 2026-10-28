package com.kwinse.plant_monitor_api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Plain unit tests: no Spring, no database. Calibration is node 1's real values (2900 air, 900 water).
class DeviceTests {

    private Device device;

    @BeforeEach
    void setUp() {
        device = new Device();
        device.setDryRaw(2900);
        device.setWetRaw(900);
    }

    @Test
    void dryCalibrationIsZeroPercent() {
        assertThat(device.moisturePercent(2900)).isEqualTo(0.0);
    }

    @Test
    void wetCalibrationIsHundredPercent() {
        assertThat(device.moisturePercent(900)).isEqualTo(100.0);
    }

    @Test
    void midpointIsFiftyPercent() {
        assertThat(device.moisturePercent(1900)).isEqualTo(50.0);
    }

    @Test
    void drierThanCalibrationClampsToZero() {
        assertThat(device.moisturePercent(3200)).isEqualTo(0.0);
    }

    @Test
    void wetterThanCalibrationClampsToHundred() {
        assertThat(device.moisturePercent(700)).isEqualTo(100.0);
    }

    @Test
    void roundsToOneDecimal() {
        // (2900 - 1234) / 2000 * 100 = 83.3
        assertThat(device.moisturePercent(1234)).isEqualTo(83.3);
    }
}
