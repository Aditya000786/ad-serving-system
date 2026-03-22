package com.adserving.events;

import com.adserving.core.model.AdEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private final EventDeduplicator deduplicator;
    private final EventEnricher enricher;
    private final ReportingAggregator reportingAggregator;
    private final ObjectMapper objectMapper;

    public EventConsumer(EventDeduplicator deduplicator,
                         EventEnricher enricher,
                         ReportingAggregator reportingAggregator,
                         ObjectMapper objectMapper) {
        this.deduplicator = deduplicator;
        this.enricher = enricher;
        this.reportingAggregator = reportingAggregator;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ad-events", groupId = "event-processor")
    public void consume(String message) {
        try {
            AdEvent event = objectMapper.readValue(message, AdEvent.class);

            // 1. Deduplication
            if (deduplicator.isDuplicate(event)) {
                log.debug("Duplicate event skipped: {}", event.getEventId());
                return;
            }

            // 2. Enrichment
            AdEvent enriched = enricher.enrich(event);

            // 3. Aggregation for reporting
            reportingAggregator.aggregate(enriched);

            log.debug("Processed event: {} type={} ad={}",
                    event.getEventId(), event.getType(), event.getAdId());
        } catch (Exception e) {
            log.error("Failed to process event: {}", message, e);
        }
    }
}
