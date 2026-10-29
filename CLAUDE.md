# CLAUDE.md

Plant monitor: ESP32 (C/C++) sensor nodes -> Spring Boot REST API -> MySQL. Portfolio project for a job search. Every design decision must be explainable by the owner in an interview. See README.md for run instructions, endpoints, and known gaps, and docs/SPEC.md for goals, design decisions, and status.

## Quality bar

Company-grade, not "make it work." Before adding any field, column, or endpoint, answer:
- **Who sets it:** client or server? Clients never set ids, timestamps, or computed values (request records, not entities, as `@RequestBody`).
- **What's valid:** missing, wrong type, out of range, boundary, weird-but-real.
- **Null:** can it be null, and what does null mean?
- **When it's wrong:** what breaks, and who finds out? Errors are loud: the right status code (400 client input, 401 auth, 404 missing resource, 409 state conflict, 500 our bug), logged with context, never swallowed in a catch.
- **Who else reads or writes it:** search references; don't rely on memory.

Show the failure for real (remove the check, run it) before calling it fixed.

## Commands

- Start stack: `docker compose up -d --build` (needs `.env`, see README)
- Dev loop: `docker compose watch` (rebuilds the API on changes to `src/`)
- Logs: `docker compose logs -f backend`
- Tests: `./mvnw test` (needs Docker running; Testcontainers starts MySQL). CI runs the same on push/PR.
- Simulator: `python device-client/simulator/send_reading.py`

The app is run only through Docker Compose. There is no IDE/local-JVM run path, which is deliberate so there's only one way to run things. `application.properties` intentionally has no datasource URL; Compose injects it through `SPRING_DATASOURCE_*` env vars. MySQL is on host port **3307**, not 3306.

## Code layout

- `src/main/java/com/kwinse/plant_monitor_api/`: flat package. `ApiKeyFilter` (servlet filter, constant-time key compare). Entities `Device` (calibration), `Plant`, `DeviceAssignment` (which plant a device is in, and when), `Reading`, each with a Spring Data repository and a controller. `*Request` records define what clients may send.
- `device-client/esp32-firmware/`: Arduino sketches (C/C++). MicroPython was dropped on purpose; don't reintroduce it.
- `device-client/simulator/`: Python fake device.

## Rules

- Never read, print, or commit `.env` values.
- Any security-relevant change (auth, validation) gets a test that fails without the change. Verify that by temporarily reverting the change.
- Spring Boot 4.x: test annotations live in the new packages (e.g. `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`), and mocks use `@MockitoBean`, not `@MockBean`.
- The project spec is `docs/SPEC.md`. After a real milestone or a reversed decision, update its Status (Section 9) and add a Decisions row (Section 5) in the same format. Ask the owner for their reasoning rather than inventing it.
