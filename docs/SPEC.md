# Plant Health Monitoring System

*Project Spec & Roadmap — Portfolio / Learning Project*

**Last updated: October 26, 2026**

**Status: Backend running in Docker Compose with tests + CI. Firmware reads sensors and drives the screen locally but does not send to the API yet. Living document, expected to go stale as the build progresses.**

## How to use this document

- This is the only spec for this project. If a second version ever shows up somewhere else, that's a bug: delete or merge it, don't let both exist. (Moved into the repo on 2026-09-25 so it's versioned alongside the code. The old external copy should be deleted.)
- Trust the "Last updated" date, not the content, by default. If it's been more than a few real work sessions since that date, assume Section 5 (Decisions) and Section 9 (Status) are out of date until checked against what's actually true right now.
- Update ritual: at every real checkpoint (firmware milestone, backend up, a decision reversed), spend 2 minutes updating Section 9 and adding a row to Section 5 if something changed. Small and often beats a big rewrite later.
- If this doc contradicts current reality in a future conversation, say so immediately. Treat the contradiction as the doc doing its job, not as a mistake. Best guess as of its date, not locked truth.

## 1. Why This Exists

This project started as a want to add credible C/C++ experience to a resume for early-career applications at Lockheed Martin, without it reading as language collecting (a line item with no depth behind it). A portfolio project only works as proof if every decision in it can be explained on demand, not just demoed.

The plant angle was chosen because it has real stakes (a living thing that visibly succeeds or fails based on whether the system works) and gives a reason to add a screen and mood visual, which makes the device legible and interesting instead of a bare sensor dashboard.

**The real problem (added 2026-10-26).** Over the last year the owner kept killing plants, just more slowly: they survive instead of thriving. The common advice is "don't water on a schedule, stick your finger in and decide," but that takes experience: what exactly is dry enough? Some plants want water while still a little damp. Cheap 3-for-$15 probe meters made it worse, reading "very dry" in soil that was clearly damp, which led to overwatering and root rot. A single number means nothing without trends, a baseline for that specific plant (species, pot, soil, root health), and events like repotting. The goal is "a sensor that takes the things a beginner wouldn't know, or that are subjective, so I can make an objective decision based on data and learn over time."

The scope grew naturally instead of being padded: firmware needed a backend to send data to, a backend needs a frontend to be checkable, and historical data is only useful if something validates whether the original care assumptions were even correct. Each layer below earns its place for that reason.

## 2. Scope

### In scope (v1)

- One ESP32 node per plant, each with 1 moisture + 1 light sensor, no screen (changed 2026-10-25, see Section 5).
- C/C++ firmware: reads sensors, sends raw readings over WiFi/HTTP. The Tamagotchi-style mood moves to the dashboard for now.
- Java Spring Boot backend: REST API, MySQL storage.
- JS frontend dashboard: shows current + historical state per plant, pulled from the Spring Boot API.
- Python analytics layer: pulls historical data, checks whether initial thresholds/sensor placement were actually correct, light trend prediction.
- AWS deployment: EC2 for backend, RDS for the database, paced to nPower Cloud Practitioner course topics.

### Explicitly out of scope / parked

- Portable handheld "cyberdeck" companion device (BLE-linked, check-plants-while-traveling). Scrapped for now: soldering, battery integration, and form factor were all blockers on a first hardware project. Not deleted; revisit after this build is finished and soldering/enclosure skills exist.
- BLE entirely. It only existed to serve the handheld's no-wifi-while-traveling requirement. With no handheld, there's no reason to avoid WiFi.
- Auto-watering (pump + relay). Parts optional/shelved. Adding a pump before thresholds are trusted risks automated overwatering while nobody's watching, which is worse than the manual problem it would replace.
- 3rd ESP32 setup. Parts on hand but not used. 2 screens on one board's onboard voltage regulator was flagged as risky, so the layout was simplified to 2+1 instead.

## 3. System Architecture

Four layers, each justified above rather than padding for resume length:

- **Layer 1 — ESP32 firmware (C/C++):** sensor reads, on-device mood visual, WiFi/HTTP send.
- **Layer 2 — Java Spring Boot backend:** receives + stores readings, exposes REST API, MySQL.
- **Layer 3 — JS frontend dashboard:** displays live + historical state per plant, consumes the Spring Boot API.
- **Layer 4 — Python analytics:** validates whether original thresholds/placement assumptions were correct, light trend prediction (honest AI exposure, not overclaimed).

## 4. Hardware Layout

- One node per plant (3x ELEGOO 38-pin ESP32 on hand). Same wiring on every node, no breadboard needed:
  - Capacitive soil sensor: VCC → 3V3, GND → GND, AOUT → D34 (ADC1, works with WiFi on).
  - BH1750 light sensor: VCC → D25 (GPIO set HIGH in code, since the board has only one 3V3 pin; D4 did not power it), GND → second GND, SDA → D21, SCL → D22, ADDR unconnected (0x23).
- Node 1 built 2026-10-25. Moisture calibration (sensor on 3V3): ~2900 in air, ~900 in water.
- BH1750 header is taped, not soldered. Holds when untouched; twisting or pressing causes failed reads. Solder when an iron is available.
- USB wall power. Battery parked: the TP4056 module is charge-only (outputs raw 3.7–4.2V), so a battery would also need a ≥500mA low-dropout 3.3V regulator, deep-sleep firmware, and the dev board's power LED disabled to last more than ~1–2 weeks.
- The 38-pin board is too wide for a single breadboard (no free holes beside its pins); a desk test setup needs two breadboards side by side.

## 5. Key Design Decisions

Format: the realistic alternative first (why someone would reasonably pick it), then the specific mechanism of why it breaks down for this project, then the honest cost of the choice actually made. This is the format used for interview prep, not just documentation, so it should hold up under a "but why not X" follow-up.

| Decision | Realistic Alternative | Why It Breaks Down Here | Honest Tradeoff of Choice Made |
| --- | --- | --- | --- |
| WiFi/HTTP over BLE | BLE: no internet dependency, works while traveling, lower power draw, was the original plan for a portable handheld. | The handheld cyberdeck that BLE existed to serve got scrapped (soldering + battery + form factor were blockers as a first hardware project). Sensor nodes now stay plugged in at home, in wifi range, permanently. BLE's main advantage (no-wifi range independence) has no use case left to justify its complexity. | WiFi means the ESP32 depends on the home network being up and reachable. If wifi drops, readings stop landing in the backend until it reconnects. Acceptable here since the device never leaves the house. |
| C/C++ (Arduino framework) over MicroPython | MicroPython: faster to prototype, easier debugging, no manual memory management, was the original firmware plan. | The project's stated purpose is building a resume-credible C/C++ signal for Lockheed Martin early-career applications. MicroPython would ship the same product but produce zero C/C++ portfolio evidence, defeating the reason the project exists. | C/C++ costs more debugging time (manual memory, no REPL, slower iteration loop) and requires understanding stack vs heap, pointer lifetime, and struct layout that MicroPython would have hidden. That cost is the point, not a side effect. |
| Struct over separate variables for sensor readings | Separate primitives (moisture, light, timestamp as loose variables): simpler for a beginner, no dot-notation or struct syntax to learn. | The moment a second plant/sensor exists, separate variables force duplicated names (moisture1, moisture2) and every function touching sensor data needs either two versions or a pile of parameters. Struct also has no enforced relationship between values in the separate-variable version: nothing stops reading moisture2 alongside light1 by mistake. | Struct carries marginally more memory overhead than raw primitives. On ESP32 with RAM to spare this doesn't bite, but it is a real, nameable cost if this ever targeted a memory-starved chip. |
| Java Spring Boot backend (reused skill) over a new language/framework | Node/Express or Python/FastAPI: faster to write, more common in small IoT hobby projects, would add a second new language. | Java is an existing resume skill. Using a new backend framework here would mean learning two unfamiliar things at once (C/C++ firmware and a new backend) with no reinforcement of what's already claimed on the resume. Reusing Java turns this project into proof the existing skill is real, not just listed. | Spring Boot has more setup ceremony (dependency injection, annotations, project structure) than a lightweight framework would for a project this small. That overhead is accepted because the goal is reinforcing depth in Java, not minimizing lines of code. |
| MySQL over PostgreSQL | PostgreSQL: richer feature set (JSONB, extensions, stricter SQL standards compliance), popular default for new projects, and what this project originally ran on. | Nothing technical breaks down: at this workload (a few readings per minute) both are far more than enough. The deciding factor is the target job market: Java/Spring Boot listings pair with MySQL often, and this project exists to produce evidence for those listings. Switching was cheap (JPA abstracts most SQL). | Gives up Postgres-only features that could help later (e.g. JSONB for flexible sensor payloads). If the analytics layer needs advanced SQL, Postgres would have been the stronger fit. This is a career-driven choice, not a technical one, and it's stated as such. |
| Docker Compose as the only way to run the backend | Run Spring Boot from the IDE against a Dockerized DB: native breakpoints, faster restarts. | Two run paths means two sets of config (ports, credentials) that drift apart. After months away, the IDE path failed (wrong port, no credentials) while Compose still worked. One path means one thing to remember. | No IDE breakpoints by default, and `docker compose watch` rebuilds take ~30s versus a near-instant IDE restart. |
| Testcontainers over H2 for integration tests | H2 in-memory DB: no Docker needed, faster startup, zero setup. | H2 is a different database engine from MySQL. SQL dialect, types, and constraint behavior differ, so tests can pass on H2 and fail in production. The project already migrated away from H2 once. | Tests require Docker running, and the first run is slower (~15s to start MySQL). CI runners provide Docker, so this only costs local convenience. |
| One ESP32 node per plant (2026-10-25) | 2+1 split: one ESP32 serving 2 plants on a shared power rail, plus one standalone node. Fewer boards. | "Keep it simple and make it work first." The shared node meant more wiring on one board, an open shared-screen question, and wires running to two pots. One node per plant is the same wiring built once and repeated. | Uses all 3 ESP32s with no spare, and every plant needs its own USB power near the pot. |
| No screen on plant nodes (2026-10-25) | Keep the Tamagotchi OLED on each node, which is the original reason the device is "legible and interesting" (Section 1). | "I don't really need the screen working, I need data over time." The screen added wiring and power draw without adding anything to the dataset the analytics layer depends on. | The at-a-glance mood on the device is gone for now and moves to the dashboard. The project loses some of its on-device personality until a screen comes back. |
| Send all useful fields: deviceId, moistureRaw, light, rssi (2026-10-25) | Send only moisture, which the backend already accepted. | "Easier to send as much data as possible and configure the backend to take what we need." Moisture-only would mix every plant into one stream once a 2nd node exists, and throw away light and WiFi signal data. | The backend needs a column for every field. Spring silently ignores JSON fields the entity doesn't have, so "send everything" only helps if the schema keeps up. |
| Convert raw moisture to % once, in the backend (2026-10-25) | Convert on the ESP32 before sending, or have each consumer (dashboard, analytics) convert raw itself. | Convert once in the backend, and the frontend and database can just consume it without worrying. If the backend doesn't convert, you have to worry about it everywhere it gets converted. Converting on the ESP32 would also mean re-flashing each node to re-calibrate. | More backend work: a calibration per device and the conversion code, and validation checks the raw range (0–4095) instead of 0–100. |
| Separate `devices`, `plants`, and `device_assignments` tables (2026-10-26) | One table: readings just store a device ID and the device "is" the plant. | "Since I'm a plant killer I could see myself needing to move the device to a different plant," even a cutting of the same species, and "each plant is unique too, even the same breed in different pots." The device is the tool, the plant is the thing being measured. A `devices` table with a FK ties every reading to a source of truth; assignments record which plant a device was in, and when. Moving a sensor is a database change, not a re-flash. | Three tables and a join where one would do on day one. Someone has to record each move (close the old assignment, open a new one), or readings land on the wrong plant. |
| Stamp `plant_id` on each reading at insert time (2026-10-26) | Readings store only `device_id`; find a plant's readings by joining through `device_assignments` on date ranges. Fully normalized: fixing an assignment date automatically fixes every reading. | "I'd rather write a simple query, which would be easier on the system to load." Charts get read far more often than readings get written, so the lookup happens once per reading instead of on every dashboard load. | Denormalized: if an assignment date is entered wrong, the affected readings keep the wrong `plant_id` until fixed with an `UPDATE`. Accepted because moves are rare and the fix is one query. |
| Keep readings from an unassigned device, with `plant_id` NULL (2026-10-27) | Reject the reading (`400`) so every saved row has a plant and no query has to handle NULL. | "Save it until you fix," because data records should be retained. The ESP32 doesn't buffer failed sends, so a rejected reading is gone forever, while a NULL can be fixed later with one `UPDATE`. NULL is also the honest value: "we don't know which plant," same as `light: null`. | `plant_id` is nullable, so every query that groups or filters by plant has to think about NULL rows. |
| Devices, plants, and assignments created through API endpoints, not hand-written SQL (2026-10-27) | Insert the handful of rows by hand in a MySQL shell. Rare setup, zero code. | Endpoints "can be reused," and every write goes through the same API key check and validation. A hand insert skips both, so a typo like `wetRaw = 9000` silently makes every % from that device wrong. Moving a device is also two writes (end the old assignment, start the new one), which the endpoint does in one transaction. | More code and tests (three controllers) for something done a few times a year. Business logic lives in controllers for now; a service layer would be the next step if it grows. |
| AWS (EC2 + RDS) timed to nPower AWS Cloud Practitioner course schedule | Deploy immediately on whatever's fastest (Heroku, Render, local Docker) and skip AWS until the course requires it. | Cloud concepts (EC2, RDS, storage) are being taught in the nPower course concurrently. Deploying to AWS now means every concept gets applied hands-on on a real project as it's taught, instead of being labbed in isolation and forgotten before it's used for real. | AWS free tier has real limits and EC2/RDS setup has more moving parts (VPC, security groups, IAM) than a one-click host. Slower to get a first deploy working, but that friction is exactly what the course is teaching. **Update 2026-09-25:** accounts created after July 15, 2025 get credits and a free plan that ends at 6 months or when credits run out, so AWS is a learning/demo deployment, not a permanent free host. Always-on hosting stays on a local machine for now. |

## 6. Data Flow

- ESP32 reads moisture + light on an interval, updates the local mood visual immediately (no network dependency for the on-device experience).
- ESP32 sends a JSON payload over HTTP to the Spring Boot API (matches the struct shape defined in firmware, see Section 5).
- Spring Boot validates and writes the reading to MySQL, keyed by plant/sensor node ID and timestamp.
- JS dashboard polls or requests the API for current + historical readings, renders per-plant state.
- Python analytics runs against the same MySQL data on a separate cadence, not real-time, since it's evaluating trends and threshold correctness, not live status.

## 7. Security Considerations

- ESP32 to backend traffic is on the local home network. HTTPS is a stretch goal, not required for a device that never leaves the LAN, but it should be noted explicitly as a known gap rather than an oversight if asked in an interview. **This stops being optional the moment the API is exposed publicly (e.g. AWS).**
- All endpoints require an `X-API-KEY` header, compared in constant time. A missing-header bypass introduced by the timing-attack fix was caught and fixed on 2026-09-25, with regression tests (`ApiKeyFilterTests`).
- Spring Boot API: needs basic input validation on incoming sensor payloads (reject out-of-range values) so a misbehaving sensor can't silently poison the historical dataset the analytics layer depends on. (Moisture 0–100 is validated today.)
- AWS: EC2 security groups should restrict inbound traffic to only the ports the API and SSH need. RDS should not be publicly accessible, only reachable from the EC2 instance. This is also literally the Week 3 nPower course material, so it doubles as applied practice.
- No PII involved anywhere in this system, meaning the security bar here is about protecting data integrity (trustworthy readings) rather than confidentiality.

## 8. Failure Modes — How This Can Break

- Shared power rail on ESP32 #1 (2 sensor sets): a voltage sag or short from one sensor set can corrupt or destabilize readings from the other. Isolate wiring per sensor set even though they share the rail.
- WiFi dropout: if the home network blips, readings stop reaching the backend until reconnect. Firmware should handle this gracefully (retry/backoff) rather than crash or hang.
- Loose or taped sensor connections: the BH1750 returns failed reads when its taped header is twisted or pressed. Firmware should retry and send `light: null` instead of a fake value, so bad contact never poisons the dataset. Real fix: solder the header.
- Miscalibrated thresholds: this is explicitly why analytics (Layer 4) exists. Wrong thresholds silently produce a wrong mood/status for weeks unless something checks the assumption against reality.
- AWS free tier limits: RDS/EC2 free tier caps could be hit if left running carelessly or misconfigured. Monitor usage (set an AWS Budgets alert), don't treat free tier as unlimited.
- Host machine asleep or rebooted: containers restart automatically (`restart: unless-stopped`) only if Docker Desktop itself starts on login. If the PC is asleep, nothing is listening and readings are lost.

## 9. Status & Estimate

No hard deadline. Progress is tracked as GitHub Issues with estimates vs actuals.

- **Done:** hardware layout finalized (2+1 split, power rail plan, screen placement except the shared-screen question on ESP32 #1).
- **Done:** Spring Boot API + MySQL running via Docker Compose with auto-restart; API key auth with regression tests; Testcontainers integration test; GitHub Actions CI; README.
- **Done (2026-10-25):** Node 1 wired (moisture + light, no screen) and verified with `sensor_check.ino` (0 failed light reads when untouched) and `i2c_scanner.ino`. Moisture calibrated (~2900 air, ~900 water).
- **Done (2026-10-27):** Backend schema: `devices` (calibration), `plants`, `device_assignments`, and readings with `device_id` + stamped `plant_id`, indexed by (plant, time) and (device, time). Accepts the new payload (deviceId, moistureRaw, light, rssi) and converts raw to % per device. Validation tests for every field, verified to fail with the checks removed. `main` is branch-protected: changes go through a PR with passing CI.
- **Next:** Firmware sends readings over WiFi every minute while testing (15 min once stable), with secrets in a git-ignored `secrets.h`. Then Python ETL/analytics, then dashboard, then AWS deploy.
- Paced against the nPower course for the AWS layer specifically.

## 10. Future Enhancements (Parked, Not Forgotten)

- Portable BLE-linked handheld cyberdeck, once soldering/enclosure/battery skills exist from a completed project.
- Auto-watering via pump + relay, once sensor thresholds have weeks of trusted data behind them.
- 3rd ESP32 node, if a 3rd plant needs monitoring and the shared-screen question on ESP32 #1 gets resolved first.
- LoRa radio for true long-range, only relevant if the portable version returns and needs range beyond BLE.

## 11. Success Criteria

- Every design decision in this document can be explained out loud, including the rejected alternative and why it was rejected, not just "it worked."
- Analytics layer produces a real answer to "were the original thresholds correct," not a placeholder metric.
- All 4 layers deployed and talking to each other on AWS by the time the nPower course reaches its deployment-relevant modules.
- Every layer maps to a specific, nameable resume claim (C/C++, Java/Spring, JS, Python analytics, AWS) with a working project behind it.

## 12. Open Questions

- Shared screen on ESP32 #1: one screen cycling between 2 plants, or drop the second screen and represent plant 2 status only on the dashboard? (`sensor_test.ino` currently cycles one screen between both plants.)
- Polling interval for sensor reads: balance between responsive mood updates and not spamming the backend/wearing sensors.
- Whether HTTPS/auth on the ESP32 to backend link gets added now or stays a documented gap for v1.
- Permanent always-on host: current PC (must not sleep), Raspberry Pi / old laptop, or paid cloud after AWS credits expire?
- ~~Data model: how plants, devices, and readings relate.~~ Resolved 2026-10-26, see Section 5.
- `plant_events` table (watered, repotted, roots trimmed) to mark care events on charts. Parked until readings flow.
