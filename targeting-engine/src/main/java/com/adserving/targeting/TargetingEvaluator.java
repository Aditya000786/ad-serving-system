package com.adserving.targeting;

import com.adserving.core.model.AdRequest;
import com.adserving.core.model.TargetingCriteria;
import com.adserving.core.targeting.TargetingMatcher;

import java.util.List;

public class TargetingEvaluator {

    private final List<TargetingMatcher> matchers;

    public TargetingEvaluator(List<TargetingMatcher> matchers) {
        this.matchers = matchers;
    }

    public boolean isEligible(TargetingCriteria criteria, AdRequest request) {
        return matchers.stream().allMatch(m -> m.matches(criteria, request));
    }

    public List<String> getFailedDimensions(TargetingCriteria criteria, AdRequest request) {
        return matchers.stream()
                .filter(m -> !m.matches(criteria, request))
                .map(TargetingMatcher::dimensionName)
                .toList();
    }
}
