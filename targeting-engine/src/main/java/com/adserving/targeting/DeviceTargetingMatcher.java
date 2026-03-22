package com.adserving.targeting;

import com.adserving.core.model.AdRequest;
import com.adserving.core.model.TargetingCriteria;
import com.adserving.core.targeting.TargetingMatcher;

import java.util.List;

public class DeviceTargetingMatcher implements TargetingMatcher {

    @Override
    public String dimensionName() {
        return "device";
    }

    @Override
    public boolean matches(TargetingCriteria criteria, AdRequest request) {
        List<String> devices = criteria.getDevices();
        if (devices == null || devices.isEmpty()) {
            return true;
        }
        if (request.getDevice() == null) {
            return false;
        }
        return devices.stream()
                .anyMatch(dev -> dev.equalsIgnoreCase(request.getDevice()));
    }
}
