package com.adserving.targeting;

import com.adserving.core.model.AdRequest;
import com.adserving.core.model.TargetingCriteria;
import com.adserving.core.targeting.TargetingMatcher;

import java.util.List;

public class ContextualTargetingMatcher implements TargetingMatcher {

    @Override
    public String dimensionName() {
        return "category";
    }

    @Override
    public boolean matches(TargetingCriteria criteria, AdRequest request) {
        List<String> categories = criteria.getCategories();
        if (categories == null || categories.isEmpty()) {
            return true;
        }
        if (request.getCategory() == null) {
            return false;
        }
        return categories.stream()
                .anyMatch(cat -> cat.equalsIgnoreCase(request.getCategory()));
    }
}
