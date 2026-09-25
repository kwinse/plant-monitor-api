# Plant Health Monitoring System

*Project Spec & Roadmap — Portfolio / Learning Project*

**Last updated: September 25, 2026**

**Status: Backend running in Docker Compose with tests + CI. Firmware reads sensors and drives the screen locally but does not send to the API yet. Living document, expected to go stale as the build progresses.**

## How to use this document

- This is the only spec for this project. If a second version ever shows up somewhere else, that's a bug: delete or merge it, don't let both exist. (Moved into the repo on 2026-09-25 so it's versioned alongside the code. The old external copy should be deleted.)
- Trust the "Last updated" date, not the content, by default. If it's been more than a few real work sessions since that date, assume Section 5 (Decisions) and Section 9 (Status) are out of date until checked against what's actually true right now.
- Update ritual: at every real checkpoint (firmware milestone, backend up, a decision reversed), spend 2 minutes updating Section 9 and adding a row to Section 5 if something changed. Small and often beats a big rewrite later.
- If this doc contradicts current reality in a future conversation, say so immediately. Treat the contradiction as the doc doing its job, not as a mistake. Best guess as of its date, not locked truth.

## 1. Why This Exists

This project started as a want to add credible C/C++ experience to a resume for early-career applications at Lockheed Martin, without it reading as language collecting (a line item with no depth behind it). A portfolio project only works as proof if every decision in it can be explained on demand, not just demoed.

The plant angle was chosen because it has real stakes (a living thing that visibly succeeds or fails based on whether the system works) and gives a reason to add a screen and mood visual, which makes the device legible and interesting instead of a bare sensor dashboard.

The scope grew naturally instead of being padded: firmware needed a backend to send data to, a backend needs a frontend to be checkable, and historical data is only useful if something validates whether the original care assumptions were even correct. Each layer below earns its place for that reason.

## 2. Scope

### In scope (v1)

- 2x ESP32 nodes: one board running 2 sensor sets (moisture + light) for 2 plants sharing a power rail, one board running 1 sensor set + local screen on its own power rail.
- C/C++ firmware: reads sensors, renders a Tamagotchi-style mood on the local screen, sends readings over WiFi/HTTP.
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

- ESP32 #1: 2x moisture sensor, 2x light sensor, serving 2 nearby plants. 1 shared screen, decision pending. Shared breadboard power rail.
- ESP32 #2: 1x moisture sensor, 1x light sensor, 1x screen. Standalone. Separate power rail, same breadboard.
- No battery required for either node. Both stay plugged into wall power since the portable/traveling use case is parked.
- Parts for a 3rd setup exist but are unused, kept as spare/future stock.

## 5. Key Design Decisions

Format: the realistic alternative first (why someone would reasonably pick it), then the specific mechanism of why it breaks down for this project, then the honest cost of the choice actually made. This is the format used for interview prep, not just documentation, so it should hold up under a "but why not X" follow-up.

| Decision | Realistic Alternative | Why It Breaks Down Here | Honest Tradeoff of Choice Made |
| --- | --- | --- | --- |
| WiFi/HTTP over BLE | BLE: no internet dependency, works while traveling, lower power draw, was the original plan for a portable handheld. | The handheld cyberdeck that BLE existed to serve got scrapped (soldering + battery + form factor were blockers as a first hardware project). Sensor nodes now stay plugged in at home, in wifi range, permanently. BLE's main advantage (no-wifi range independence) has no use case left to justify its complexity. | WiFi means the ESP32 depends on the home network being up and reachable. If wifi drops, readings stop landing in the backend until it reconnects. Acceptable here since the device never leaves the house. |
| C/C++ (Arduino framework) over MicroPython | MicroPython: faster to prototype, easier debugging, no manual memory management, was the original firmware plan. | The project's stated purpose is building a resume-credible C/C++ signal for Lockheed Martin early-career applications. MicroPython would ship the same product but produce zero C/C++ portfolio evidence, defeating the reason the project exists. | C/C++ costs more debugging time (manual memory, no REPL, slower iteration loop) and requires understanding stack vs heap, pointer lifetime, and struct layout that MicroPython would have hidden. That cost is the point, not a side effect. |
| Struct over separate variables for sensor readings | Separate primitives (moisture, light, timestamp as loose variables): simpler for a beginner, no dot-notation or struct syntax to learn. | The moment a second plant/sensor exists, separate variables force duplicated names (moisture1, moisture2) and every function touching sensor data needs either two versions or a pile of parameters. Struct also has no enforced relationship between values in the separate-variable version: nothing stops reading moisture2 alongside light1 by mistake. | Struct carries marginally more memory overhead than raw primitives. On ESP32 with RAM to spare this doesn't bite, but it is a real, nameable cost if this ever targeted a memory-starved chip. |
| Java Spring Boot backend (reused skill) over a new language/framework | Node/Express or Python/FastAPI: faster to write, more common in small IoT hobby projects, would add a second new language. | Java is an existing resume skill. Using a new backend framework here would mean learning two unfamiliar things at once (C/C++ firmware and a new backend) with no reinforcement of what's already claimed on the resume. Reusing Java turns this project into proof the existing skill is real, not just listed. | Spring Boot has more setup ceremony (dependency injection, annotations, project structure) than a lightweight framework would for a project this small. That overhead is accepted because the goal is reinforcing depth in Java, not minimizing lines of code. |
| MySQL over PostgreSQL | *TODO (owner): fill in. The project ran on Postgres until commit a24967c. Why switch?* | *TODO* | *TODO* |
| Docker Compose as the only way to run the backend | Run Spring Boot from the IDE against a Dockerized DB: native breakpoints, faster restarts. | Two run paths means two sets of config (ports, credentials) that drift apart. After months away, the IDE path failed (wrong port, no credentials) while Compose still worked. One path means one thing to remember. | No IDE breakpoints by default, and `docker compose watch` rebuilds take ~30s versus a near-instant IDE restart. |
| Testcontainers over H2 for integration tests | H2 in-memory DB: no Docker needed, faster startup, zero setup. | H2 is a different database engine from MySQL. SQL dialect, types, and constraint behavior differ, so tests can pass on H2 and fail in production. The project already migrated away from H2 once. | Tests require Docker running, and the first run is slower (~15s to start MySQL). CI runners provide Docker, so this only costs local convenience. |
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
- Miscalibrated thresholds: this is explicitly why analytics (Layer 4) exists. Wrong thresholds silently produce a wrong mood/status for weeks unless something checks the assumption against reality.
- AWS free tier limits: RDS/EC2 free tier caps could be hit if left running carelessly or misconfigured. Monitor usage (set an AWS Budgets alert), don't treat free tier as unlimited.
- Host machine asleep or rebooted: containers restart automatically (`restart: unless-stopped`) only if Docker Desktop itself starts on login. If the PC is asleep, nothing is listening and readings are lost.

## 9. Status & Estimate

No hard deadline. Progress is tracked as GitHub Issues with estimates vs actuals.

- **Done:** hardware layout finalized (2+1 split, power rail plan, screen placement except the shared-screen question on ESP32 #1).
- **Done:** Spring Boot API + MySQL running via Docker Compose with auto-restart; API key auth with regression tests; Testcontainers integration test; GitHub Actions CI; README.
- **In progress:** ESP32 firmware. `sensor_test.ino` reads 2 moisture + 2 light sensors and shows mood on the OLED. Next: send readings over WiFi to the API.
- **Next:** Plant/Device data model (readings are currently one unkeyed stream), then Python ETL/analytics, then dashboard, then AWS deploy.
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
- Data model: how plants, devices (ESP32 boards), sensors, and readings relate. Needed before firmware sends real data.
