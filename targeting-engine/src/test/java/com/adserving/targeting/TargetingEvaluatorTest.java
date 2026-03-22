package com.adserving.targeting;

import com.adserving.core.model.AdRequest;
import com.adserving.core.model.GeoTargeting;
import com.adserving.core.model.TargetingCriteria;
import com.adserving.core.targeting.TargetingMatcher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TargetingEvaluatorTest {

    @Test
    void allMatchersPass() {
        var evaluator = new TargetingEvaluator(List.of(
                new GeoTargetingMatcher(),
                new ContextualTargetingMatcher(),
                new DeviceTargetingMatcher()
        ));

        var criteria = TargetingCriteria.builder()
                .geo(GeoTargeting.builder().countries(List.of("US")).build())
                .categories(List.of("technology"))
                .devices(List.of("MOBILE"))
                .build();
        var request = AdRequest.builder()
                .geo("US").category("technology").device("MOBILE")
                .build();

        assertTrue(evaluator.isEligible(criteria, request));
    }

    @Test
    void oneMatcherFails() {
        var evaluator = new TargetingEvaluator(List.of(
                new GeoTargetingMatcher(),
                new ContextualTargetingMatcher()
        ));

        var criteria = TargetingCriteria.builder()
                .geo(GeoTargeting.builder().countries(List.of("US")).build())
                .categories(List.of("finance"))
                .build();
        var request = AdRequest.builder()
                .geo("US").category("technology")
                .build();

        assertFalse(evaluator.isEligible(criteria, request));
    }

    @Test
    void getFailedDimensions() {
        var evaluator = new TargetingEvaluator(List.of(
                new GeoTargetingMatcher(),
                new ContextualTargetingMatcher(),
                new DeviceTargetingMatcher()
        ));

        var criteria = TargetingCriteria.builder()
                .geo(GeoTargeting.builder().countries(List.of("IN")).build())
                .categories(List.of("finance"))
                .devices(List.of("MOBILE"))
                .build();
        var request = AdRequest.builder()
                .geo("US").category("technology").device("MOBILE")
                .build();

        var failed = evaluator.getFailedDimensions(criteria, request);
        assertEquals(2, failed.size());
        assertTrue(failed.contains("geo"));
        assertTrue(failed.contains("category"));
    }

    @Test
    void emptyMatchersAlwaysEligible() {
        var evaluator = new TargetingEvaluator(List.of());
        var criteria = TargetingCriteria.builder().build();
        var request = AdRequest.builder().build();
        assertTrue(evaluator.isEligible(criteria, request));
    }
}
