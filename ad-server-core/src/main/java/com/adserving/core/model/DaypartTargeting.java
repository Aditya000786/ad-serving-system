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
public class DaypartTargeting {
    private List<Integer> hours;
    private List<String> days;
    private String timezone;
}
