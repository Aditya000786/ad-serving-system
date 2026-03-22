package com.adserving.events;

import com.adserving.core.model.AdEvent;
import com.adserving.core.model.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
public class ReportingAggregator {

    private static final Logger log = LoggerFactory.getLogger(ReportingAggregator.class);
    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH")
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneOffset.UTC);

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbcTemplate;

    public ReportingAggregator(StringRedisTemplate redis, JdbcTemplate jdbcTemplate) {
        this.redis = redis;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void aggregate(AdEvent event) {
        Instant ts = event.getTimestamp() != null ? event.getTimestamp() : Instant.now();
        String hourKey = HOUR_FMT.format(ts);
        String dateKey = DATE_FMT.format(ts);

        String counterPrefix = "stats:" + event.getCampaignId() + ":" + event.getAdId();

        switch (event.getType()) {
            case IMPRESSION -> {
                redis.opsForHash().increment(counterPrefix + ":hourly:" + hourKey, "impressions", 1);
                redis.opsForHash().increment(counterPrefix + ":daily:" + dateKey, "impressions", 1);
            }
            case CLICK -> {
                redis.opsForHash().increment(counterPrefix + ":hourly:" + hourKey, "clicks", 1);
                redis.opsForHash().increment(counterPrefix + ":daily:" + dateKey, "clicks", 1);
            }
            case CONVERSION -> {
                redis.opsForHash().increment(counterPrefix + ":hourly:" + hourKey, "conversions", 1);
                redis.opsForHash().increment(counterPrefix + ":daily:" + dateKey, "conversions", 1);
            }
        }
    }

    @Scheduled(fixedRate = 60_000) // flush to Postgres every minute
    public void flushToPostgres() {
        Instant now = Instant.now();
        String currentHour = HOUR_FMT.format(now);
        // Flush the previous hour to avoid race conditions
        String flushHour = HOUR_FMT.format(now.minus(1, ChronoUnit.HOURS));
        String flushDate = DATE_FMT.format(now);

        var keys = redis.keys("stats:*:hourly:" + flushHour);
        if (keys == null || keys.isEmpty()) {
            return;
        }

        int flushed = 0;
        for (String key : keys) {
            try {
                // key format: stats:{campaignId}:{adId}:hourly:{hour}
                String[] parts = key.split(":");
                if (parts.length < 5) continue;
                String campaignId = parts[1];
                String adId = parts[2];

                var counters = redis.opsForHash().entries(key);
                long impressions = parseLong(counters.get("impressions"));
                long clicks = parseLong(counters.get("clicks"));
                long conversions = parseLong(counters.get("conversions"));

                if (impressions + clicks + conversions == 0) continue;

                jdbcTemplate.update("""
                    INSERT INTO hourly_stats (campaign_id, ad_id, hour, impressions, clicks, conversions, spend)
                    VALUES (?, ?, ?::timestamp, ?, ?, ?, 0)
                    ON CONFLICT (campaign_id, ad_id, hour) DO UPDATE SET
                        impressions = EXCLUDED.impressions,
                        clicks = EXCLUDED.clicks,
                        conversions = EXCLUDED.conversions
                    """,
                        campaignId, adId, flushHour + ":00:00",
                        impressions, clicks, conversions);
                flushed++;
            } catch (Exception e) {
                log.error("Failed to flush stats for key {}", key, e);
            }
        }

        if (flushed > 0) {
            log.debug("Flushed {} hourly stat entries to PostgreSQL", flushed);
        }
    }

    private long parseLong(Object val) {
        if (val == null) return 0;
        return Long.parseLong(val.toString());
    }
}
