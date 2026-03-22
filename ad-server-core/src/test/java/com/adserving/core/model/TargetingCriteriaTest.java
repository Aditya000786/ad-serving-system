package com.adserving.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TargetingCriteriaTest {

    @Test
    void shouldBuildTargetingCriteriaWithAllFields() {
        GeoTargeting geo = GeoTargeting.builder()
                .countries(List.of("US", "CA"))
                .cities(List.of("New York", "Toronto"))
                .build();

        DaypartTargeting dayparting = DaypartTargeting.builder()
                .hours(List.of(9, 10, 11, 12, 13, 14, 15, 16, 17))
                .days(List.of("MON", "TUE", "WED", "THU", "FRI"))
                .timezone("America/New_York")
                .build();

        TargetingCriteria criteria = TargetingCriteria.builder()
                .geo(geo)
                .devices(List.of("MOBILE", "DESKTOP"))
                .categories(List.of("technology", "finance"))
                .dayparting(dayparting)
                .build();

        assertNotNull(criteria);
        assertEquals(2, criteria.getGeo().getCountries().size());
        assertTrue(criteria.getGeo().getCountries().contains("US"));
        assertEquals(2, criteria.getGeo().getCities().size());
        assertEquals(2, criteria.getDevices().size());
        assertEquals(2, criteria.getCategories().size());
        assertEquals(9, criteria.getDayparting().getHours().size());
        assertEquals(5, criteria.getDayparting().getDays().size());
        assertEquals("America/New_York", criteria.getDayparting().getTimezone());
    }

    @Test
    void shouldBuildTargetingCriteriaWithNullFields() {
        TargetingCriteria criteria = TargetingCriteria.builder().build();

        assertNotNull(criteria);
        assertNull(criteria.getGeo());
        assertNull(criteria.getDevices());
        assertNull(criteria.getCategories());
        assertNull(criteria.getDayparting());
    }

    @Test
    void shouldBuildTargetingCriteriaWithOnlyGeo() {
        GeoTargeting geo = GeoTargeting.builder()
                .countries(List.of("US"))
                .build();

        TargetingCriteria criteria = TargetingCriteria.builder()
                .geo(geo)
                .build();

        assertNotNull(criteria);
        assertEquals(1, criteria.getGeo().getCountries().size());
        assertNull(criteria.getGeo().getCities());
        assertNull(criteria.getDevices());
    }

    @Test
    void shouldSupportEqualsAndHashCode() {
        TargetingCriteria criteria1 = TargetingCriteria.builder()
                .devices(List.of("MOBILE"))
                .categories(List.of("technology"))
                .build();

        TargetingCriteria criteria2 = TargetingCriteria.builder()
                .devices(List.of("MOBILE"))
                .categories(List.of("technology"))
                .build();

        assertEquals(criteria1, criteria2);
        assertEquals(criteria1.hashCode(), criteria2.hashCode());
    }
}
