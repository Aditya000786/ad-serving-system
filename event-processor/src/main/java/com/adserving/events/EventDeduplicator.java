package com.adserving.events;

import com.adserving.core.model.AdEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class EventDeduplicator {

    private static final Duration DEDUP_TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "event:seen:";

    private final StringRedisTemplate redis;

    public EventDeduplicator(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean isDuplicate(AdEvent event) {
        String key = KEY_PREFIX + event.getEventId();
        Boolean wasAbsent = redis.opsForValue().setIfAbsent(key, "1", DEDUP_TTL);
        return wasAbsent == null || !wasAbsent;
    }
}
