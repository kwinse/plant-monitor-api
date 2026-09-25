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
Posts a random moisture value every 5 seconds and prints the latest reading. It reads `API_KEY` from the root `.env`.

### Quick manual check
```bash
curl -X POST localhost:8080/api/readings \
  -H "X-API-KEY: <your key>" -H "Content-Type: application/json" \
  -d '{"moisture": 42.5}'
```

## API

Every endpoint requires an `X-API-KEY` header. A missing or wrong key returns `401`.

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/readings` | Body `{"moisture": 0-100}`. Server sets the timestamp. `400` if missing or out of range. |
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
./mvnw test -Dtest=ApiKeyFilterTests
```
`ApiKeyFilterTests` checks that missing, wrong, and correct API keys are handled correctly. It runs without a database.

The generated `PlantMonitorApiApplicationTests` needs a live database and currently fails under a plain `./mvnw test`. See known gaps.

## Known gaps

- `PlantMonitorApiApplicationTests` fails without a database, so `./mvnw test` is not green yet.
- The ESP32 doesn't send data to the API yet.
- Readings aren't tied to a plant or device yet. Everything is one stream.
- The API uses plain HTTP with one shared key. That's acceptable on a home LAN, but it must change before any public deployment.
- `GET /api/readings/{id}` returns `500` instead of `404` for an unknown id.
- Timestamp format uses `YYYY` (week-based year) instead of `yyyy`.
