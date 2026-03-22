package com.adserving.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdResponse {
    private String adId;
    private String title;
    private String creativeUrl;
    private String clickUrl;
    private String impressionUrl;
    private AuctionInfo auction;
}
