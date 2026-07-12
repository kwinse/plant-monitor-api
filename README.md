# Plant Monitor API

Backend for a plant sensor project. Device sends a moisture reading, API checks it's valid and authorized, saves it to Postgres. Basic IoT pipeline shape — sensor → API → database.

ESP32 hardware isn't wired up yet, so right now a Python script fakes the sensor and sends mock readings to the live API.

## Architecture

```
ESP32 (MicroPython, in progress)
        |
        v
Python device simulator (random mock readings)
        |
        v
Spring Boot REST API  ---> API Key auth filter (rejects unauthorized requests)
        |
        v
Validation (rejects out-of-range moisture values)
        |
        v
PostgreSQL (running in Docker, persists across restarts)
```

## Tech stack

- **Java 17 / Spring Boot** — REST API, dependency injection, request filtering
- **Spring Data JPA / Hibernate** — ORM layer, maps Java objects to database tables
- **PostgreSQL** — persistent storage, containerized via Docker
- **Docker** — isolated, reproducible local database environment
- **Python** — device simulator (soon to be replaced/supplemented by real ESP32 firmware)

## Features

- `POST /api/readings` — submit a new sensor reading (validates moisture is between 0–100)
- `GET /api/readings` — retrieve all readings
- `GET /api/readings/{id}` — retrieve a single reading by ID
- `GET /api/readings/by-moisture/{moisture}` — filter readings by exact moisture value
- `GET /api/readings/by-moisture-and-timestamp` — filter by moisture and a timestamp range (query params: `moisture`, `start`, `end`)
- All endpoints require a valid `X-API-KEY` header
## Running locally

1. Start the database:
```bash
   docker run --name plant-monitor-db --env-file .env -p 5432:5432 -d postgres:17
```
2. Set up `.env` in the project root:
```
   POSTGRES_PASSWORD=your-password
   POSTGRES_DB=plant_monitor
   POSTGRES_PORT=5432
   API_KEY=your-api-key
```
3. Run the Spring Boot app (via IntelliJ or `./mvnw spring-boot:run`)
4. Run the device simulator to send mock readings:
```bash
   cd device-simulator
   python send_reading.py
```

## Next

- Port the Python sender logic to MicroPython, run it on the actual ESP32
- Add a Device entity so multiple sensors can report in
- Maybe temperature/humidity later
