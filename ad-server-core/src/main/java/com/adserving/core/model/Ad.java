package com.adserving.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ad {
    private String id;
    private String adGroupId;
    private String title;
    private String description;
    private String creativeUrl;
    private String clickUrl;
    private AdStatus status;

    // Transient fields populated during ad selection
    private String campaignId;
    private long bidAmountCents;
    private TargetingCriteria targetingCriteria;
}
