#include <Wire.h>
#include <BH1750.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64

Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, -1);
BH1750 lightMeter1(0x23);
BH1750 lightMeter2(0x5C);

const int MOISTURE_PINS[] = {34, 35};
const int DRY_THRESHOLD = 3000;
const int LOW_LIGHT_THRESHOLD = 10;  // lux, adjust once you know your plants' real range

struct PlantReading {
  int moisture;
  float light;
  String mood;
};

PlantReading plants[2];

String getMood(int moisture, float light) {
  bool dry = moisture > DRY_THRESHOLD;
  bool dark = light < LOW_LIGHT_THRESHOLD;

  if (dry && dark) return "Struggling";
  if (dry) return "Thirsty!";
  if (dark) return "Sun-starved";
  return "Happy :)";
}

void setup() {
  Serial.begin(115200);
  Wire.begin();
  lightMeter1.begin(BH1750::CONTINUOUS_HIGH_RES_MODE, 0x23);
  lightMeter2.begin(BH1750::CONTINUOUS_HIGH_RES_MODE, 0x5C);

  if (!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) {
    Serial.println("Screen not found");
    while (true);
  }
}

void loop() {
  plants[0].moisture = analogRead(MOISTURE_PINS[0]);
  plants[0].light = lightMeter1.readLightLevel();
  plants[0].mood = getMood(plants[0].moisture, plants[0].light);

  plants[1].moisture = analogRead(MOISTURE_PINS[1]);
  plants[1].light = lightMeter2.readLightLevel();
  plants[1].mood = getMood(plants[1].moisture, plants[1].light);

  for (int i = 0; i < 2; i++) {
    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SSD1306_WHITE);
    display.setCursor(0, 0);
    display.print("Plant ");
    display.println(i + 1);

    display.setTextSize(2);
    display.setCursor(0, 20);
    display.println(plants[i].mood);

    display.setTextSize(1);
    display.setCursor(0, 50);
    display.print("M:");
    display.print(plants[i].moisture);
    display.print(" L:");
    display.print(plants[i].light);

    display.display();

    Serial.print("Plant ");
    Serial.print(i + 1);
    Serial.print(" - Moisture: ");
    Serial.print(plants[i].moisture);
    Serial.print(" Light: ");
    Serial.print(plants[i].light);
    Serial.print(" Mood: ");
    Serial.println(plants[i].mood);

    delay(3000);
  }
}