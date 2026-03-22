package com.adserving.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventContext {
    private String geo;
    private String device;
    private String pageUrl;
    private String ipHash;
}
