package com.adserving.api.controller;

import com.adserving.core.model.AdRequest;
import com.adserving.core.model.AdResponse;
import com.adserving.api.service.AdSelectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class AdSelectionController {

    private final AdSelectionService adSelectionService;

    public AdSelectionController(AdSelectionService adSelectionService) {
        this.adSelectionService = adSelectionService;
    }

    @GetMapping("/ad")
    public ResponseEntity<AdResponse> selectAd(
            @RequestParam(required = false) String geo,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String device,
            @RequestParam(name = "user_id", required = false) String userId) {

        String requestId = UUID.randomUUID().toString();

        AdRequest request = AdRequest.builder()
                .geo(geo)
                .city(city)
                .category(category)
                .device(device)
                .userId(userId)
                .build();

        Optional<AdResponse> response = adSelectionService.selectAd(request);

        if (response.isEmpty()) {
            return ResponseEntity.noContent()
                    .header("X-Request-Id", requestId)
                    .build();
        }

        return ResponseEntity.ok()
                .header("X-Request-Id", requestId)
                .body(response.get());
    }
}
