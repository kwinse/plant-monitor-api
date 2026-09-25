# CLAUDE.md

Plant monitor: ESP32 (C/C++) sensor nodes -> Spring Boot REST API -> MySQL. Portfolio project for a job search. Every design decision must be explainable by the owner in an interview. See README.md for run instructions, endpoints, and known gaps, and docs/SPEC.md for goals, design decisions, and status.

## Working with the owner

The owner is a junior dev who is building real understanding while moving fast. Pace work in three tiers:
1. **Trivial** (conditionals, loops, renames, obvious fixes): just do it.
2. **Unfamiliar patterns** (Spring annotations, JPA, Docker, Arduino libs): do it, then briefly explain what each non-obvious piece does and why it exists.
3. **Decisions and trade-offs** (schema, auth, hosting, libraries, error-handling strategy): stop before coding. Present options with honest costs, ask for the owner's pick and reasoning first, then recommend. Build only after agreement.

When the owner could plausibly find a bug themselves, point at the location and ask before revealing the answer.

## Commands

- Start stack: `docker compose up -d --build` (needs `.env`, see README)
- Dev loop: `docker compose watch` (rebuilds the API on changes to `src/`)
- Logs: `docker compose logs -f backend`
- Tests: `./mvnw test` (needs Docker running; Testcontainers starts MySQL). CI runs the same on push/PR.
- Simulator: `python device-client/simulator/send_reading.py`

The app is run only through Docker Compose. There is no IDE/local-JVM run path, which is deliberate so there's only one way to run things. `application.properties` intentionally has no datasource URL; Compose injects it through `SPRING_DATASOURCE_*` env vars. MySQL is on host port **3307**, not 3306.

## Code layout

- `src/main/java/com/kwinse/plant_monitor_api/`: flat package. `ApiKeyFilter` (servlet filter, constant-time key compare), `Reading` (JPA entity), `ReadingRepository` (Spring Data derived queries), `ReadingController`.
- `device-client/esp32-firmware/`: Arduino sketches (C/C++). MicroPython was dropped on purpose; don't reintroduce it.
- `device-client/simulator/`: Python fake device.

## Rules

- Never read, print, or commit `.env` values.
- Any security-relevant change (auth, validation) gets a test that fails without the change. Verify that by temporarily reverting the change.
- Spring Boot 4.x: test annotations live in the new packages (e.g. `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`), and mocks use `@MockitoBean`, not `@MockBean`.
- The project spec is `docs/SPEC.md`. After a real milestone or a reversed decision, update its Status (Section 9) and add a Decisions row (Section 5) in the same format. Ask the owner for their reasoning rather than inventing it.
