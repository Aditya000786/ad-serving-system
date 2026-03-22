package com.adserving.events;

import com.adserving.core.model.AdEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventEnricher {

    private final StringRedisTemplate redis;

    public EventEnricher(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public AdEvent enrich(AdEvent event) {
        // Enrich with campaign data from Redis cache if campaignId is missing
        if (event.getCampaignId() == null || event.getCampaignId().isEmpty()) {
            String campaignId = redis.opsForValue().get("ad:campaign:" + event.getAdId());
            if (campaignId != null) {
                event.setCampaignId(campaignId);
            }
        }
        return event;
    }
}
