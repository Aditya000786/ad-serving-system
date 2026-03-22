package com.adserving.core.targeting;

import com.adserving.core.model.AdRequest;
import com.adserving.core.model.TargetingCriteria;

public interface TargetingMatcher {
    boolean matches(TargetingCriteria criteria, AdRequest request);
    String dimensionName();
}
