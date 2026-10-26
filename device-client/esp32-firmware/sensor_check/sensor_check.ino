#include <Wire.h>
#include <BH1750.h>

// Quick wiring check for one plant node: 1 moisture sensor + 1 light sensor, no screen.
// Prints both readings every sec                                             ond and counts failed light reads, so a bad connection shows up as numbers.

const int MOISTURE_PIN = 34;     // ADC1 pin: ADC2 pins stop working while WiFi is on
// Calibration (capacitive sensor powered from 3V3): ~2900 in air (driest), ~900 in water (wettest).
const int LIGHT_POWER_PIN = 25;  // BH1750 VCC comes from this GPIO (only 1 3V3 pin on the board). D4 didn't power it; D25 does.

BH1750 lightMeter(0x23);         // ADDR left unconnected = 0x23

int readCount = 0;
int failedLightReads = 0;

void setup() {
  Serial.begin(115200);

  pinMode(LIGHT_POWER_PIN, OUTPUT);
  digitalWrite(LIGHT_POWER_PIN, HIGH);   // power the light sensor
  delay(200);                            // let it start up before talking to it

  Wire.begin();                          // SDA = 21, SCL = 22 by default
  if (!lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE)) {
    Serial.println("Light sensor not found: check VCC (pin 4), GND, SDA (21), SCL (22)");
  }
}

void loop() {
  int moisture = analogRead(MOISTURE_PIN);
  float light = lightMeter.readLightLevel();   // negative = the read failed

  readCount++;
  if (light < 0) {
    failedLightReads++;
  }

  Serial.print("Moisture: ");
  Serial.print(moisture);
  Serial.print("   Light: ");
  if (light < 0) {
    Serial.print("FAILED");
  } else {
    Serial.print(light);
    Serial.print(" lux");
  }
  Serial.print("   Failed light reads: ");
  Serial.print(failedLightReads);
  Serial.print("/");
  Serial.println(readCount);

  delay(1000);
}
