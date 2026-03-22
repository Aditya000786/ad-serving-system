package com.adserving.targeting;

import com.adserving.core.model.AdRequest;
import com.adserving.core.model.TargetingCriteria;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContextualTargetingMatcherTest {

    private final ContextualTargetingMatcher matcher = new ContextualTargetingMatcher();

    @Test
    void matchesCategory() {
        var criteria = TargetingCriteria.builder().categories(List.of("technology", "finance")).build();
        var request = AdRequest.builder().category("technology").build();
        assertTrue(matcher.matches(criteria, request));
    }

    @Test
    void noCategoriesPasses() {
        var criteria = TargetingCriteria.builder().build();
        var request = AdRequest.builder().category("technology").build();
        assertTrue(matcher.matches(criteria, request));
    }

    @Test
    void wrongCategoryFails() {
        var criteria = TargetingCriteria.builder().categories(List.of("finance")).build();
        var request = AdRequest.builder().category("sports").build();
        assertFalse(matcher.matches(criteria, request));
    }

    @Test
    void caseInsensitive() {
        var criteria = TargetingCriteria.builder().categories(List.of("TECHNOLOGY")).build();
        var request = AdRequest.builder().category("technology").build();
        assertTrue(matcher.matches(criteria, request));
    }

    @Test
    void nullCategoryInRequestFails() {
        var criteria = TargetingCriteria.builder().categories(List.of("tech")).build();
        var request = AdRequest.builder().build();
        assertFalse(matcher.matches(criteria, request));
    }
}
