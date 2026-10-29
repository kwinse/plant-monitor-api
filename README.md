# Plant Monitor

A plant health monitoring system: ESP32 sensor nodes measure soil moisture and light, show a "mood" on a small screen, and report readings to a Spring Boot API backed by MySQL.

Built as a portfolio project to get hands-on experience across embedded C/C++, Java/Spring Boot, Docker, and (later) AWS.

## Current status

| Layer | State |
|---|---|
| Spring Boot API + MySQL (Docker Compose) | Working |
| Python device simulator | Working, sends fake readings |
| ESP32 firmware (C/C++, Arduino) | Reads 2 moisture + 2 light sensors and shows mood on an OLED. **Does not send to the API yet.** |
| JS dashboard | Not started |
| Python analytics | Not started |
| AWS deployment | Not started. Currently runs on a local machine. |

## Architecture

```
ESP32 firmware (C/C++)          Python simulator
  moisture + light sensors        fake readings every 5s
  mood on OLED screen                   |
        |  (WiFi/HTTP, not yet)         |
        v                               v
   +------------------------------------------+
   |  Spring Boot API  (container, port 8080) |
   |   ApiKeyFilter -> rejects bad/missing key |
   |   ReadingController -> validates 0-100    |
   +------------------------------------------+
                      |
                      v
        MySQL 8  (container, host port 3307)
        data persists in a Docker volume
```

## Running it

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) running
- A `.env` file in the project root (gitignored, never commit it):

```
MYSQL_ROOT_PASSWORD=pick-a-password
MYSQL_DATABASE=plant_monitor
API_KEY=pick-a-long-random-key
```

### Start everything
```bash
docker compose up -d --build
```
This builds the API image, starts MySQL, waits until it is healthy, then starts the API on `http://localhost:8080`.

Both containers use `restart: unless-stopped`, so they come back after a crash or reboot as long as Docker Desktop is running. (Docker Desktop → Settings → General → *Start Docker Desktop when you sign in* makes this survive reboots.)

### Everyday commands
| What | Command |
|---|---|
| Is it running? | `docker compose ps` |
| API logs | `docker compose logs -f backend` |
| Auto-rebuild on code change | `docker compose watch` |
| Stop (data kept) | `docker compose down` |
| Stop and **wipe the database** | `docker compose down -v` |
| Open a MySQL shell | `docker exec -it plant-monitor-db mysql -uroot -p plant_monitor` |

### Send fake readings
```bash
pip install requests python-dotenv
python device-client/simulator/send_reading.py
```
Posts a random raw reading as device `1` every 5 seconds and prints the latest reading. It reads `API_KEY` from the root `.env`. Device 1 must be registered first (see below).

### First-time setup: register a device and a plant
```bash
post() { curl -X POST "localhost:8080$1" -H "X-API-KEY: <your key>" -H "Content-Type: application/json" -d "$2"; echo; }
post /api/devices     '{"label": "NODE-01", "dryRaw": 2900, "wetRaw": 900}'
post /api/plants      '{"name": "Pink Princess", "species": "Philodendron erubescens"}'
post /api/assignments '{"deviceId": 1, "plantId": 1}'
post /api/readings    '{"deviceId": 1, "moistureRaw": 1900, "light": 120.5, "rssi": -60}'
```

## API

Every endpoint requires an `X-API-KEY` header. A missing or wrong key returns `401`.

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/readings` | Body `{"deviceId", "moistureRaw": 0-4095, "light": 0-65535 or null, "rssi": -120-0 or null}`. Server converts raw to moisture % using the device's calibration, stamps the plant the device is currently assigned to (`null` if unassigned), and sets the timestamp. `400` if a field is missing, out of range, or the device is unknown. |
| `POST` | `/api/devices` | Body `{"label", "dryRaw", "wetRaw"}`. Raw values 0-4095, `dryRaw > wetRaw`. `409` if the label exists. |
| `GET` | `/api/devices` | All devices |
| `POST` | `/api/plants` | Body `{"name", "species"?, "dateAcquired"?, "targetMoistureMin"?, "targetMoistureMax"?}`. Targets 0-100, min ≤ max. |
| `GET` | `/api/plants` | All plants |
| `POST` | `/api/assignments` | Body `{"deviceId", "plantId"}`. Moves the device: ends its current assignment and starts a new one, in one transaction. `409` if already on that plant. |
| `GET` | `/api/assignments` | Assignment history |
| `GET` | `/api/readings/all` | All readings |
| `GET` | `/api/readings/latest` | Most recent reading, `204` if none |
| `GET` | `/api/readings/{id}` | One reading |
| `GET` | `/api/readings/by-moisture/{moisture}` | Exact moisture match |
| `GET` | `/api/readings/by-moisture-and-timestamp?moisture=&start=&end=` | `start`/`end` as ISO date-time, e.g. `2026-09-25T00:00:00` |

## Firmware

`device-client/esp32-firmware/sensor_test/sensor_test.ino`, built with the Arduino IDE for ESP32.

- Libraries: `BH1750`, `Adafruit GFX`, `Adafruit SSD1306`
- Moisture sensors on GPIO 34 and 35 (analog). Two BH1750 light sensors on I2C at `0x23` and `0x5C`. SSD1306 OLED at `0x3C`.
- Cycles the screen between plants every 3 seconds and also logs to Serial at 115200 baud.

## Tests

```bash
./mvnw test
```
Docker must be running. `PlantMonitorApiApplicationTests` uses [Testcontainers](https://testcontainers.com/) to start a throwaway MySQL 8 container, so tests run against the same database engine as production; it also runs one full device → plant → assignment → reading flow. `ApiKeyFilterTests` checks missing, wrong, and correct API keys. `ReadingValidationTests`, `DeviceControllerTests`, `PlantControllerTests`, and `DeviceAssignmentControllerTests` cover each field's missing / out-of-range / boundary cases with mocked repositories. `DeviceTests` covers the raw-to-% conversion.

GitHub Actions runs the full suite on every push and pull request to `main` (`.github/workflows/ci.yml`).

## Known gaps

- The ESP32 doesn't send data to the API yet.
- If a device is moved without recording a new assignment, its readings are charted on the old plant. The system can't detect a move on its own.
- The API uses plain HTTP with one shared key. That's acceptable on a home LAN, but it must change before any public deployment.
- `GET /api/readings/{id}` returns `500` instead of `404` for an unknown id.
