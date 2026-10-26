#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64

Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, -1);

const int MOISTURE_PIN = 34;     // ADC1 pin: ADC2 pins stop working while WiFi is on
const int DRY_THRESHOLD = 3000;  // raw ADC, higher = drier. Placeholder until calibrated.

struct PlantReading {
  int moisture;
  const char* mood;  // points at a fixed string literal, no heap allocation
};

PlantReading plant;

const char* getMood(int moisture) {
  if (moisture > DRY_THRESHOLD) return "Thirsty!";
  return "Happy :)";
}

void setup() {
  Serial.begin(115200);
  Wire.begin();

  if (!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) {
    Serial.println("Screen not found");
    while (true);
  }
}

void loop() {
  plant.moisture = analogRead(MOISTURE_PIN);
  plant.mood = getMood(plant.moisture);

  display.clearDisplay();
  display.setTextColor(SSD1306_WHITE);

  display.setTextSize(2);
  display.setCursor(0, 20);
  display.println(plant.mood);

  display.setTextSize(1);
  display.setCursor(0, 50);
  display.print("M:");
  display.print(plant.moisture);

  display.display();

  Serial.print("Moisture: ");
  Serial.print(plant.moisture);
  Serial.print(" Mood: ");
  Serial.println(plant.mood);

  delay(3000);
}
