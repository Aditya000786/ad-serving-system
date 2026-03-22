package com.adserving.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "ads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdEntity {

    @Id
    private String id;

    @Column(name = "ad_group_id")
    private String adGroupId;

    private String title;
    private String description;

    @Column(name = "creative_url")
    private String creativeUrl;

    @Column(name = "click_url")
    private String clickUrl;

    private String status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
