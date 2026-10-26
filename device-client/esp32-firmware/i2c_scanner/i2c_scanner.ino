#include <Wire.h>

// Lists every I2C device that answers on SDA (21) / SCL (22).
// BH1750 should show up at 0x23 (or 0x5C if its ADDR pin is tied to 3.3V). OLED would be 0x3C.

const int LIGHT_POWER_PIN = 25;   // same as the plant node: BH1750 VCC comes from this GPIO

void setup() {
  Serial.begin(115200);
  pinMode(LIGHT_POWER_PIN, OUTPUT);
  digitalWrite(LIGHT_POWER_PIN, HIGH);
  delay(200);
  Wire.begin();
}

void loop() {
  int found = 0;
  Serial.println("Scanning...");
  for (byte address = 1; address < 127; address++) {
    Wire.beginTransmission(address);
    if (Wire.endTransmission() == 0) {   // 0 = a device acknowledged this address
      Serial.print("  Found device at 0x");
      Serial.println(address, HEX);
      found++;
    }
  }
  if (found == 0) {
    Serial.println("  No I2C devices found");
  }
  delay(3000);
}
