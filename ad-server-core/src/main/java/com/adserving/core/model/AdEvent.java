package com.adserving.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdEvent {
    private String eventId;
    private EventType type;
    private String adId;
    private String campaignId;
    private String userId;
    private Instant timestamp;
    private EventContext context;
}
