package com.adserving.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {
    private String id;
    private String name;
    private String advertiserId;
    private long dailyBudgetCents;
    private long totalBudgetCents;
    private CampaignStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Instant createdAt;
    private Instant updatedAt;
}
