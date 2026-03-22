package com.adserving.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "ad_groups")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdGroupEntity {

    @Id
    private String id;

    @Column(name = "campaign_id")
    private String campaignId;

    private String name;

    @Column(name = "targeting_criteria", columnDefinition = "jsonb")
    private String targetingCriteria;

    @Column(name = "bid_amount_cents")
    private long bidAmountCents;

    @Column(name = "bid_type")
    private String bidType;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
