package com.adserving.budget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;

@Component
public class PacingAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(PacingAlgorithm.class);
    private static final int SLOTS_PER_DAY = 144; // 10-minute slots
    private static final double MIN_MULTIPLIER = 0.1;
    private static final double MAX_MULTIPLIER = 3.0;

    private final StringRedisTemplate redis;

    public PacingAlgorithm(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Scheduled(fixedRate = 10_000) // every 10 seconds
    public void updatePacingMultipliers() {
        Set<String> campaignKeys = redis.keys("budget:*:remaining");
        if (campaignKeys == null || campaignKeys.isEmpty()) {
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        int currentSlot = getCurrentSlot(now);
        double elapsedFraction = getElapsedSlotFraction(now);

        for (String key : campaignKeys) {
            String campaignId = key.replace("budget:", "").replace(":remaining", "");
            updatePacingForCampaign(campaignId, currentSlot, elapsedFraction);
        }
    }

    private void updatePacingForCampaign(String campaignId, int currentSlot, double elapsedFraction) {
        String dailyKey = "budget:" + campaignId + ":daily_remaining";
        String dailyVal = redis.opsForValue().get(dailyKey);
        if (dailyVal == null) {
            return;
        }

        long dailyRemaining = Long.parseLong(dailyVal);

        // Get original daily budget from the campaign's initial daily budget key
        String origDailyKey = "budget:" + campaignId + ":daily_budget";
        String origVal = redis.opsForValue().get(origDailyKey);
        if (origVal == null) {
            return;
        }
        long dailyBudget = Long.parseLong(origVal);
        long actualSpend = dailyBudget - dailyRemaining;

        // Expected spend: uniform distribution across slots
        double expectedSpendRatio = (currentSlot + elapsedFraction) / SLOTS_PER_DAY;
        double expectedSpend = dailyBudget * expectedSpendRatio;

        // Calculate multiplier
        double multiplier;
        if (actualSpend <= 0) {
            multiplier = MAX_MULTIPLIER; // haven't spent anything, boost
        } else {
            multiplier = expectedSpend / actualSpend;
        }

        // Clamp
        multiplier = Math.min(MAX_MULTIPLIER, Math.max(MIN_MULTIPLIER, multiplier));

        // Record spend in current slot
        String slotKey = "pacing:" + campaignId + ":slot_spend:" + currentSlot;
        redis.opsForValue().set("pacing:" + campaignId + ":multiplier",
                String.valueOf(multiplier));

        log.debug("Campaign {} pacing: expected={}, actual={}, multiplier={}",
                campaignId, expectedSpend, actualSpend, multiplier);
    }

    private int getCurrentSlot(ZonedDateTime now) {
        int minuteOfDay = now.getHour() * 60 + now.getMinute();
        return minuteOfDay / 10; // 10-minute slots
    }

    private double getElapsedSlotFraction(ZonedDateTime now) {
        int minuteInSlot = now.getMinute() % 10;
        int secondInSlot = now.getSecond();
        return (minuteInSlot * 60 + secondInSlot) / 600.0;
    }
}
