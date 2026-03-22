package com.adserving.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdGroup {
    private String id;
    private String campaignId;
    private String name;
    private TargetingCriteria targetingCriteria;
    private long bidAmountCents;
    private BidType bidType;
}
