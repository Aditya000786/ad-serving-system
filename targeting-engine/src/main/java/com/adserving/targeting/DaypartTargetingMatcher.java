package com.adserving.targeting;

import com.adserving.core.model.AdRequest;
import com.adserving.core.model.DaypartTargeting;
import com.adserving.core.model.TargetingCriteria;
import com.adserving.core.targeting.TargetingMatcher;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class DaypartTargetingMatcher implements TargetingMatcher {

    private static final Map<String, DayOfWeek> DAY_MAP = Map.of(
            "MON", DayOfWeek.MONDAY,
            "TUE", DayOfWeek.TUESDAY,
            "WED", DayOfWeek.WEDNESDAY,
            "THU", DayOfWeek.THURSDAY,
            "FRI", DayOfWeek.FRIDAY,
            "SAT", DayOfWeek.SATURDAY,
            "SUN", DayOfWeek.SUNDAY
    );

    @Override
    public String dimensionName() {
        return "daypart";
    }

    @Override
    public boolean matches(TargetingCriteria criteria, AdRequest request) {
        DaypartTargeting dayparting = criteria.getDayparting();
        if (dayparting == null) {
            return true;
        }

        boolean hasHours = dayparting.getHours() != null && !dayparting.getHours().isEmpty();
        boolean hasDays = dayparting.getDays() != null && !dayparting.getDays().isEmpty();

        if (!hasHours && !hasDays) {
            return true;
        }

        ZoneId zone = ZoneId.of("UTC");
        if (dayparting.getTimezone() != null && !dayparting.getTimezone().isEmpty()) {
            try {
                zone = ZoneId.of(dayparting.getTimezone());
            } catch (Exception e) {
                // Fall back to UTC on invalid timezone
            }
        }

        ZonedDateTime now = ZonedDateTime.now(zone);

        if (hasHours && !dayparting.getHours().contains(now.getHour())) {
            return false;
        }

        if (hasDays) {
            DayOfWeek currentDay = now.getDayOfWeek();
            boolean dayMatch = dayparting.getDays().stream()
                    .map(String::toUpperCase)
                    .map(DAY_MAP::get)
                    .anyMatch(d -> d != null && d == currentDay);
            if (!dayMatch) {
                return false;
            }
        }

        return true;
    }
}
