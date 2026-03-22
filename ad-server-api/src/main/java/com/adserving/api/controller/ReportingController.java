package com.adserving.api.controller;

import com.adserving.events.ReportingService;
import com.adserving.events.ReportingService.CampaignStats;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/reports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/campaign/{id}")
    public ResponseEntity<List<CampaignStats>> getCampaignReport(
            @PathVariable String id,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "DAILY") String granularity) {

        List<CampaignStats> stats = reportingService.getHistoricalStats(id, from, to, granularity);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/campaign/{id}/realtime")
    public ResponseEntity<CampaignStats> getRealTimeStats(@PathVariable String id) {
        CampaignStats stats = reportingService.getRealTimeStats(id);
        return ResponseEntity.ok(stats);
    }
}
