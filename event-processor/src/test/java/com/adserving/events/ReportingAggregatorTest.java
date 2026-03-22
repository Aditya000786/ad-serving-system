package com.adserving.events;

import com.adserving.core.model.AdEvent;
import com.adserving.core.model.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReportingAggregatorTest {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneOffset.UTC);

    private ReportingAggregator aggregator;
    private StringRedisTemplate redis;

    @BeforeEach
    void setUp() {
        try {
            LettuceConnectionFactory factory = new LettuceConnectionFactory(
                    new RedisStandaloneConfiguration("localhost", 6379));
            factory.afterPropertiesSet();
            redis = new StringRedisTemplate(factory);
            redis.getConnectionFactory().getConnection().ping();
            // Pass null for JdbcTemplate since we only test Redis aggregation
            aggregator = new ReportingAggregator(redis, null);

            // Clean test keys
            var keys = redis.keys("stats:test-*");
            if (keys != null && !keys.isEmpty()) redis.delete(keys);
        } catch (Exception e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Redis not available");
        }
    }

    @Test
    void testImpressionAggregation() {
        AdEvent event = AdEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .type(EventType.IMPRESSION)
                .adId("test-ad-1")
                .campaignId("test-camp-1")
                .timestamp(Instant.now())
                .build();

        aggregator.aggregate(event);
        aggregator.aggregate(event); // second impression

        String dateKey = DATE_FMT.format(Instant.now());
        String key = "stats:test-camp-1:test-ad-1:daily:" + dateKey;
        Object impressions = redis.opsForHash().get(key, "impressions");
        assertEquals("2", impressions.toString());
    }

    @Test
    void testClickAggregation() {
        AdEvent event = AdEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .type(EventType.CLICK)
                .adId("test-ad-2")
                .campaignId("test-camp-2")
                .timestamp(Instant.now())
                .build();

        aggregator.aggregate(event);

        String dateKey = DATE_FMT.format(Instant.now());
        String key = "stats:test-camp-2:test-ad-2:daily:" + dateKey;
        Object clicks = redis.opsForHash().get(key, "clicks");
        assertEquals("1", clicks.toString());
    }

    @Test
    void testMixedEventTypes() {
        Instant now = Instant.now();
        String adId = "test-ad-3";
        String campId = "test-camp-3";

        // 3 impressions, 1 click, 1 conversion
        for (int i = 0; i < 3; i++) {
            aggregator.aggregate(AdEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .type(EventType.IMPRESSION)
                    .adId(adId).campaignId(campId).timestamp(now).build());
        }
        aggregator.aggregate(AdEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .type(EventType.CLICK)
                .adId(adId).campaignId(campId).timestamp(now).build());
        aggregator.aggregate(AdEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .type(EventType.CONVERSION)
                .adId(adId).campaignId(campId).timestamp(now).build());

        String dateKey = DATE_FMT.format(now);
        String key = "stats:" + campId + ":" + adId + ":daily:" + dateKey;
        assertEquals("3", redis.opsForHash().get(key, "impressions").toString());
        assertEquals("1", redis.opsForHash().get(key, "clicks").toString());
        assertEquals("1", redis.opsForHash().get(key, "conversions").toString());
    }
}
