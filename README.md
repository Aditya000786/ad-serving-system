# Ad Serving System

A production-grade ad serving system in Java demonstrating real-time ad selection, auction mechanics, budget management with pacing, event processing, and reporting. Built with the same stack used at Google Ads and Uber Ad Serving.

## Quick Start

```bash
docker-compose up -d
mvn clean package -DskipTests
java -jar ad-server-api/target/ad-server-api-1.0.0-SNAPSHOT.jar
```

The system auto-seeds **50 campaigns, 200 ad groups, and 500 ads** with realistic targeting data on first startup.

### Try it

```bash
# Get an ad
curl "http://localhost:8080/v1/ad?geo=IN&category=finance&device=DESKTOP&user_id=u123"

# Click redirect
curl -v "http://localhost:8080/v1/click?ad=<ad_id>&track=<track_token>"

# Post an event
curl -X POST "http://localhost:8080/v1/events" \
  -H "Content-Type: application/json" \
  -d '{"type":"CLICK","adId":"ad_456","campaignId":"camp_789"}'

# Real-time stats
curl "http://localhost:8080/v1/reports/campaign/<campaign_id>/realtime"
```

## Architecture

```
Ad Request ──▶ Targeting Engine ──▶ Budget Check ──▶ Auction ──▶ Ad Response
                    │                    │              │
                    ▼                    ▼              ▼
               Redis Index         Redis Lua       Kafka Events
              (SINTER sets)       (atomic deduct)  (async publish)
                                                       │
                                                       ▼
                                                 Event Processor
                                               (dedup → enrich → aggregate)
                                                       │
                                              ┌────────┼────────┐
                                              ▼                 ▼
                                        Redis Counters    PostgreSQL
                                       (real-time stats) (historical reports)
```

### Ad Selection Pipeline (< 10ms p50)

1. Parse request context (geo, category, device, user)
2. Redis `SINTER` across targeting dimension sets → candidate ad IDs
3. Fetch ad metadata (Caffeine L2 cache, 5s TTL)
4. Double-check targeting criteria (index may be stale)
5. Filter by budget availability (Redis read)
6. Apply pacing multiplier (probabilistic throttle)
7. Score & rank: `Ad Rank = bid × quality_score`
8. Second-price auction: winner pays `(rank_2 / quality_1) + $0.01`
9. Atomic budget deduction (Redis Lua script, single RTT)
10. Publish impression event to Kafka (async, fire-and-forget)
11. Return response with signed tracking URLs

## Performance

**Sustained 1,000 RPS for 60 seconds** on a single node (MacBook Pro M3):

```
  █ TOTAL RESULTS

    http_req_duration..............: avg=6.73ms min=214µs  med=3.01ms p(90)=13.16ms p(95)=20.38ms
      { expected_response:true }...: avg=5.55ms min=214µs  med=2.89ms p(90)=12.24ms p(95)=18.3ms
    http_reqs......................: 59813  996.68/s
    http_req_failed................: 1.98%

    checks_succeeded...: 98.36%
    ✓ status is 200 or 204 ↳ 98%
    ✓ response time < 50ms ↳ 98%
```

| Metric | Value |
|--------|-------|
| **p50 latency** | 3.01ms |
| **p90 latency** | 13.16ms |
| **p95 latency** | 20.38ms |
| **Throughput** | 997 req/s sustained |
| **Success rate** | 98% |
| **Total requests** | 59,813 in 60s |

Run the load test yourself:
```bash
brew install k6
k6 run load-tests/ad-selection.js
```

## Tech Stack

| Component | Technology | Why |
|-----------|-----------|-----|
| Language | Java 21 | Virtual threads for high-concurrency |
| Framework | Spring Boot 3.3 | Production-grade |
| Message Queue | Apache Kafka | Event-driven processing |
| Hot Cache | Redis | Sub-ms lookups, atomic budget ops |
| Cold Storage | PostgreSQL | Campaigns, ads, reporting |
| Build | Maven (multi-module) | Clean module boundaries |
| Containers | Docker Compose | One-command local setup |
| Load Testing | k6 | Prove performance claims |

## Modules

```
ad-serving-system/
├── ad-server-core/       # Domain models, ports/interfaces (zero infra deps)
├── targeting-engine/     # Geo, device, contextual, daypart targeting
├── budget-manager/       # Atomic budget deduction, pacing, daily reset
├── event-processor/      # Kafka consumer, dedup, enrichment, reporting
├── ad-server-api/        # REST API, Spring Boot app, adapters
└── load-tests/           # k6 performance test scripts
```

## Key Design Decisions

**Budget atomicity** — Single Redis Lua script checks both total and daily budget, deducts atomically. No overspend even under concurrent requests.

**Charge at serve time** — Budget deducted when ad is selected, not on impression confirmation. Industry standard trade-off: slight overcharge is safer than overspend.

**Pacing** — Proportional pacing (Twitter/X approach): divide day into 144 ten-minute slots, compare actual vs. expected spend, adjust serving probability. Multiplier clamped to [0.1, 3.0].

**Event deduplication** — Redis `SETNX` with 24h TTL per event ID. Guarantees at-most-once processing.

**Targeting index** — Redis sets per dimension (`geo:US`, `category:tech`, `device:MOBILE`), intersected with `SINTER`. Each set capped at 5K members.

**Track tokens** — HMAC-SHA256 signed tokens containing `{ad_id, campaign_id, user_id, geo, timestamp, nonce}` for click/impression validation and fraud detection.

## API Reference

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/ad` | GET | Ad selection (geo, category, device, user_id) |
| `/v1/click` | GET | Click redirect (302) + event tracking |
| `/v1/impression` | GET | Impression pixel + event tracking |
| `/v1/events` | POST | Generic event ingestion (202 async) |
| `/v1/reports/campaign/{id}/realtime` | GET | Real-time stats from Redis |
| `/v1/reports/campaign/{id}` | GET | Historical stats (from, to, granularity) |

## Data Model

```sql
campaigns    (id, name, advertiser_id, daily_budget_cents, total_budget_cents, status, dates)
ad_groups    (id, campaign_id, targeting_criteria JSONB, bid_amount_cents, bid_type)
ads          (id, ad_group_id, title, creative_url, click_url, status)
hourly_stats (campaign_id, ad_id, hour, impressions, clicks, conversions, spend)
daily_stats  (campaign_id, ad_id, date, impressions, clicks, conversions, spend, ctr)
```

## Evolutionary Architecture

Started as a modular monolith with clean port/adapter boundaries. Ready for microservice extraction:
- Event Processor → separate Kafka consumer service
- Auction Engine → gRPC service for sub-ms communication
- Reporting → isolated from serving path

## License

MIT
