package com.adserving.events;

import com.adserving.core.model.AdEvent;
import com.adserving.core.port.EventPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String TOPIC = "ad-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishEvent(AdEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            // Use adId as key for partition affinity (same ad events go to same partition)
            kafkaTemplate.send(TOPIC, event.getAdId(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event {}: {}", event.getEventId(), ex.getMessage());
                        } else {
                            log.debug("Published event {} to partition {}",
                                    event.getEventId(),
                                    result.getRecordMetadata().partition());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event {}", event.getEventId(), e);
        }
    }
}
