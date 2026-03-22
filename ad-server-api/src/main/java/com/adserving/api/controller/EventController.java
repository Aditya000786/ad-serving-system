package com.adserving.api.controller;

import com.adserving.core.model.AdEvent;
import com.adserving.core.model.EventType;
import com.adserving.core.port.EventPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class EventController {

    private final EventPort eventPort;

    public EventController(EventPort eventPort) {
        this.eventPort = eventPort;
    }

    @PostMapping("/events")
    public ResponseEntity<Void> receiveEvent(@RequestBody AdEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        eventPort.publishEvent(event);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/click")
    public ResponseEntity<Void> handleClick(
            @RequestParam("ad") String adId,
            @RequestParam("track") String trackToken) {

        // Publish click event
        eventPort.publishEvent(AdEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .type(EventType.CLICK)
                .adId(adId)
                .timestamp(Instant.now())
                .build());

        // In a real system, we'd decode the track token to get the click URL
        // For now, return 302 to a placeholder
        return ResponseEntity.status(302)
                .header("Location", "https://advertiser.example.com/landing?ad=" + adId)
                .build();
    }

    @GetMapping("/impression")
    public ResponseEntity<Void> handleImpression(
            @RequestParam("ad") String adId,
            @RequestParam("track") String trackToken) {

        eventPort.publishEvent(AdEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .type(EventType.IMPRESSION)
                .adId(adId)
                .timestamp(Instant.now())
                .build());

        // Return 1x1 transparent pixel
        return ResponseEntity.ok().build();
    }
}
