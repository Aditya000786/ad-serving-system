package com.adserving.api.config;

import com.adserving.core.targeting.TargetingMatcher;
import com.adserving.targeting.ContextualTargetingMatcher;
import com.adserving.targeting.DaypartTargetingMatcher;
import com.adserving.targeting.DeviceTargetingMatcher;
import com.adserving.targeting.GeoTargetingMatcher;
import com.adserving.targeting.TargetingEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class TargetingConfig {

    @Bean
    public TargetingEvaluator targetingEvaluator() {
        List<TargetingMatcher> matchers = List.of(
                new GeoTargetingMatcher(),
                new ContextualTargetingMatcher(),
                new DeviceTargetingMatcher(),
                new DaypartTargetingMatcher()
        );
        return new TargetingEvaluator(matchers);
    }
}
