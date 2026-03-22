package com.adserving.targeting;

import com.adserving.core.model.AdRequest;
import com.adserving.core.model.GeoTargeting;
import com.adserving.core.model.TargetingCriteria;
import com.adserving.core.targeting.TargetingMatcher;

import java.util.List;

public class GeoTargetingMatcher implements TargetingMatcher {

    @Override
    public String dimensionName() {
        return "geo";
    }

    @Override
    public boolean matches(TargetingCriteria criteria, AdRequest request) {
        GeoTargeting geo = criteria.getGeo();
        if (geo == null) {
            return true;
        }

        boolean countryMatch = containsIgnoreCase(geo.getCountries(), request.getGeo());
        boolean cityMatch = containsIgnoreCase(geo.getCities(), request.getCity());

        // If neither list is specified, no restriction
        boolean hasCountries = geo.getCountries() != null && !geo.getCountries().isEmpty();
        boolean hasCities = geo.getCities() != null && !geo.getCities().isEmpty();

        if (!hasCountries && !hasCities) {
            return true;
        }

        return countryMatch || cityMatch;
    }

    private boolean containsIgnoreCase(List<String> list, String value) {
        if (list == null || list.isEmpty() || value == null) {
            return false;
        }
        return list.stream().anyMatch(item -> item.equalsIgnoreCase(value));
    }
}
