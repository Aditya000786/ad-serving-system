# Ad Serving System

Real-time ad serving platform built with Java 21 and Spring Boot. Handles ad selection through a multi-stage pipeline: Redis-backed candidate retrieval, multi-dimensional targeting, budget-aware pacing, second-price auction ranking, and atomic budget deduction — all in a single request path.

## Architecture

```
                    ┌─────────────┐
   GET /v1/ad ────▶ │  API Layer  │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │  Redis   │ │Targeting │ │  Budget  │
        │  Index   │ │  Engine  │ │ Manager  │
        └──────────┘ └──────────┘ └──────────┘
              │            │            │
              └────────────┼────────────┘
                           ▼
                    ┌─────────────┐
                    │   Auction   │
                    │  (GSP/2nd)  │
                    └──────┬──────┘
                           ▼
                    ┌─────────────┐
                    │   Kafka     │
                    │  Events     │
                    └─────────────┘
```

**Modules:**

| Module | Purpose |
|--------|---------|
| `ad-server-core` | Domain models, ports, targeting matcher interfaces |
| `ad-server-api` | Spring Boot app — controllers, Redis index, JPA adapters |
| `targeting-engine` | Geo, device, contextual, and daypart targeting matchers |
| `budget-manager` | Redis-based atomic budget deduction with Lua scripts, pacing |
| `event-processor` | Kafka consumer, deduplication, reporting aggregation |

## Ad Selection Pipeline

Each request flows through these stages:

1. **Candidate retrieval** — query Redis index for eligible ad IDs matching request parameters
2. **Targeting evaluation** — filter candidates through geo, device, contextual, and daypart matchers
3. **Budget check** — verify campaign has remaining daily budget
4. **Pacing** — probabilistic throttle based on spend-vs-time pacing multiplier
5. **Auction** — score candidates, rank, apply GSP second-price pricing
6. **Budget deduction** — atomic Redis decrement via Lua script (race-condition safe)
7. **Event publishing** — fire impression event to Kafka (async, non-blocking)

## Quick Start

**Prerequisites:** Docker and Docker Compose

```bash
# Start everything (Postgres, Redis, Kafka, app)
docker-compose up -d

# Verify the app is running
curl http://localhost:8080/v1/ad?geo=US&city=new_york&category=sports&device=mobile
```

The app auto-seeds sample campaigns, ad groups, and ads on startup via Flyway migrations and data warmers.

## API Endpoints

### Ad Selection
```
GET /v1/ad?geo={geo}&city={city}&category={category}&device={device}&user_id={userId}
```
Returns the winning ad with creative URL, click/impression tracking URLs, and auction metadata.

### Event Tracking
```
GET  /v1/click?ad={adId}&track={token}       # Click redirect (302)
GET  /v1/impression?ad={adId}&track={token}   # Impression pixel
POST /v1/events                                # Raw event ingestion
```

### Reporting
```
GET /v1/reports/campaign/{id}?from={date}&to={date}&granularity={HOURLY|DAILY}
GET /v1/reports/campaign/{id}/realtime
```

## Load Test Results

Tested with [k6](https://k6.io/) using `ramping-arrival-rate` to sustain high throughput. The test ramps from 100 to 10,000 RPS over 90 seconds, then sustains peak load for 2 minutes.

```
k6 run load-test.js
```

| Metric | Result |
|--------|--------|
| Total requests | 1,676,889 |
| Throughput | ~7,000 RPS sustained |
| p50 latency | 251µs |
| p90 latency | 827µs |
| p95 latency | 2ms |
| p99 latency | 22.85ms |
| Max latency | 204ms |
| Error rate | 0.32% |
| Max VUs used | 654 |

All thresholds passed: p50 < 10ms, p95 < 50ms, p99 < 100ms, error rate < 1%.

## Tech Stack

- **Java 21** / Spring Boot 3.3
- **PostgreSQL 16** — campaign and ad metadata, reporting tables
- **Redis 7** — ad index, budget counters (Lua scripts for atomicity), pacing state
- **Kafka** — async event pipeline (impressions, clicks)
- **Caffeine** — local ad metadata cache (5s TTL)
- **Flyway** — schema migrations
- **k6** — load testing

## Local Development

```bash
# Start infrastructure only
docker-compose up -d postgres redis zookeeper kafka

# Run the app with Maven
mvn -pl ad-server-api spring-boot:run

# Run tests
mvn test

# Run load test (requires k6: brew install k6)
k6 run load-test.js
```
