package com.adserving.events;

import com.adserving.core.model.AdEvent;
import com.adserving.core.model.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventDeduplicatorTest {

    private EventDeduplicator deduplicator;
    private StringRedisTemplate redis;

    @BeforeEach
    void setUp() {
        try {
            LettuceConnectionFactory factory = new LettuceConnectionFactory(
                    new RedisStandaloneConfiguration("localhost", 6379));
            factory.afterPropertiesSet();
            redis = new StringRedisTemplate(factory);
            redis.getConnectionFactory().getConnection().ping();
            deduplicator = new EventDeduplicator(redis);

            // Clean test keys
            var keys = redis.keys("event:seen:test-*");
            if (keys != null && !keys.isEmpty()) redis.delete(keys);
        } catch (Exception e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Redis not available");
        }
    }

    @Test
    void testFirstEventIsNotDuplicate() {
        AdEvent event = AdEvent.builder()
                .eventId("test-" + UUID.randomUUID())
                .type(EventType.IMPRESSION)
                .adId("ad-1")
                .campaignId("camp-1")
                .timestamp(Instant.now())
                .build();

        assertFalse(deduplicator.isDuplicate(event));
    }

    @Test
    void testSecondEventIsDuplicate() {
        String eventId = "test-" + UUID.randomUUID();
        AdEvent event = AdEvent.builder()
                .eventId(eventId)
                .type(EventType.CLICK)
                .adId("ad-1")
                .campaignId("camp-1")
                .timestamp(Instant.now())
                .build();

        assertFalse(deduplicator.isDuplicate(event)); // first time
        assertTrue(deduplicator.isDuplicate(event));   // duplicate
    }

    @Test
    void testDifferentEventsAreNotDuplicates() {
        AdEvent event1 = AdEvent.builder()
                .eventId("test-" + UUID.randomUUID())
                .type(EventType.IMPRESSION)
                .adId("ad-1")
                .timestamp(Instant.now())
                .build();

        AdEvent event2 = AdEvent.builder()
                .eventId("test-" + UUID.randomUUID())
                .type(EventType.IMPRESSION)
                .adId("ad-1")
                .timestamp(Instant.now())
                .build();

        assertFalse(deduplicator.isDuplicate(event1));
        assertFalse(deduplicator.isDuplicate(event2));
    }
}
