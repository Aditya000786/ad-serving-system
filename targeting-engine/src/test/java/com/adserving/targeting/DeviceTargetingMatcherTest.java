package com.adserving.targeting;

import com.adserving.core.model.AdRequest;
import com.adserving.core.model.TargetingCriteria;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeviceTargetingMatcherTest {

    private final DeviceTargetingMatcher matcher = new DeviceTargetingMatcher();

    @Test
    void matchesDevice() {
        var criteria = TargetingCriteria.builder().devices(List.of("MOBILE", "DESKTOP")).build();
        var request = AdRequest.builder().device("MOBILE").build();
        assertTrue(matcher.matches(criteria, request));
    }

    @Test
    void noDeviceTargetingPasses() {
        var criteria = TargetingCriteria.builder().build();
        var request = AdRequest.builder().device("MOBILE").build();
        assertTrue(matcher.matches(criteria, request));
    }

    @Test
    void wrongDeviceFails() {
        var criteria = TargetingCriteria.builder().devices(List.of("DESKTOP")).build();
        var request = AdRequest.builder().device("MOBILE").build();
        assertFalse(matcher.matches(criteria, request));
    }

    @Test
    void caseInsensitive() {
        var criteria = TargetingCriteria.builder().devices(List.of("mobile")).build();
        var request = AdRequest.builder().device("MOBILE").build();
        assertTrue(matcher.matches(criteria, request));
    }
}
