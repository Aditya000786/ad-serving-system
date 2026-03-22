package com.adserving.targeting;

import com.adserving.core.model.AdRequest;
import com.adserving.core.model.DaypartTargeting;
import com.adserving.core.model.TargetingCriteria;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DaypartTargetingMatcherTest {

    private final DaypartTargetingMatcher matcher = new DaypartTargetingMatcher();

    @Test
    void noDaypartingPasses() {
        var criteria = TargetingCriteria.builder().build();
        var request = AdRequest.builder().build();
        assertTrue(matcher.matches(criteria, request));
    }

    @Test
    void matchesCurrentHour() {
        int currentHour = ZonedDateTime.now(ZoneId.of("UTC")).getHour();
        var criteria = TargetingCriteria.builder()
                .dayparting(DaypartTargeting.builder()
                        .hours(List.of(currentHour))
                        .timezone("UTC")
                        .build())
                .build();
        var request = AdRequest.builder().build();
        assertTrue(matcher.matches(criteria, request));
    }

    @Test
    void outsideHoursFails() {
        int currentHour = ZonedDateTime.now(ZoneId.of("UTC")).getHour();
        int differentHour = (currentHour + 12) % 24;
        var criteria = TargetingCriteria.builder()
                .dayparting(DaypartTargeting.builder()
                        .hours(List.of(differentHour))
                        .timezone("UTC")
                        .build())
                .build();
        var request = AdRequest.builder().build();
        assertFalse(matcher.matches(criteria, request));
    }

    @Test
    void matchesCurrentDay() {
        DayOfWeek today = ZonedDateTime.now(ZoneId.of("UTC")).getDayOfWeek();
        String dayAbbrev = today.name().substring(0, 3);
        var criteria = TargetingCriteria.builder()
                .dayparting(DaypartTargeting.builder()
                        .days(List.of(dayAbbrev))
                        .timezone("UTC")
                        .build())
                .build();
        var request = AdRequest.builder().build();
        assertTrue(matcher.matches(criteria, request));
    }

    @Test
    void outsideDaysFails() {
        DayOfWeek today = ZonedDateTime.now(ZoneId.of("UTC")).getDayOfWeek();
        DayOfWeek otherDay = DayOfWeek.of((today.getValue() % 7) + 1);
        String dayAbbrev = otherDay.name().substring(0, 3);
        var criteria = TargetingCriteria.builder()
                .dayparting(DaypartTargeting.builder()
                        .days(List.of(dayAbbrev))
                        .timezone("UTC")
                        .build())
                .build();
        var request = AdRequest.builder().build();
        assertFalse(matcher.matches(criteria, request));
    }

    @Test
    void emptyDaypartingPasses() {
        var criteria = TargetingCriteria.builder()
                .dayparting(DaypartTargeting.builder().build())
                .build();
        var request = AdRequest.builder().build();
        assertTrue(matcher.matches(criteria, request));
    }
}
