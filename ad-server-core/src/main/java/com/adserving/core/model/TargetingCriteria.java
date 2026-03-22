package com.adserving.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetingCriteria {
    private GeoTargeting geo;
    private List<String> devices;
    private List<String> categories;
    private DaypartTargeting dayparting;
}
