package com.adserving.targeting;

import com.adserving.core.model.AdRequest;
import com.adserving.core.model.GeoTargeting;
import com.adserving.core.model.TargetingCriteria;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoTargetingMatcherTest {

    private final GeoTargetingMatcher matcher = new GeoTargetingMatcher();

    @Test
    void matchesCountry() {
        var criteria = TargetingCriteria.builder()
                .geo(GeoTargeting.builder().countries(List.of("US", "IN")).build())
                .build();
        var request = AdRequest.builder().geo("US").build();
        assertTrue(matcher.matches(criteria, request));
    }

    @Test
    void matchesCity() {
        var criteria = TargetingCriteria.builder()
                .geo(GeoTargeting.builder().cities(List.of("San Francisco")).build())
                .build();
        var request = AdRequest.builder().city("San Francisco").build();
        assertTrue(matcher.matches(criteria, request));
    }

    @Test
    void noGeoTargetingPasses() {
        var criteria = TargetingCriteria.builder().build();
        var request = AdRequest.builder().geo("US").build();
        assertTrue(matcher.matches(criteria, request));
    }

    @Test
    void wrongCountryFails() {
        var criteria = TargetingCriteria.builder()
                .geo(GeoTargeting.builder().countries(List.of("US")).build())
                .build();
        var request = AdRequest.builder().geo("DE").build();
        assertFalse(matcher.matches(criteria, request));
    }

    @Test
    void caseInsensitive() {
        var criteria = TargetingCriteria.builder()
                .geo(GeoTargeting.builder().countries(List.of("us")).build())
                .build();
        var request = AdRequest.builder().geo("US").build();
        assertTrue(matcher.matches(criteria, request));
    }

    @Test
    void dimensionName() {
        assertEquals("geo", matcher.dimensionName());
    }
}
