package com.adserving.budget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DailyBudgetResetService {

    private static final Logger log = LoggerFactory.getLogger(DailyBudgetResetService.class);

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbcTemplate;

    public DailyBudgetResetService(StringRedisTemplate redis, JdbcTemplate jdbcTemplate) {
        this.redis = redis;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "0 0 0 * * *") // midnight UTC
    public void resetDailyBudgets() {
        log.info("Starting daily budget reset");

        List<Map<String, Object>> activeCampaigns = jdbcTemplate.queryForList(
                "SELECT id, daily_budget_cents FROM campaigns WHERE status = 'ACTIVE'");

        int count = 0;
        for (Map<String, Object> campaign : activeCampaigns) {
            String id = (String) campaign.get("id");
            long dailyBudget = ((Number) campaign.get("daily_budget_cents")).longValue();

            redis.opsForValue().set("budget:" + id + ":daily_remaining",
                    String.valueOf(dailyBudget));
            redis.opsForValue().set("budget:" + id + ":daily_budget",
                    String.valueOf(dailyBudget));

            // Clear pacing slot data
            redis.delete(redis.keys("pacing:" + id + ":slot_spend:*"));
            redis.opsForValue().set("pacing:" + id + ":multiplier", "1.0");

            count++;
        }

        log.info("Reset daily budgets for {} active campaigns", count);
    }
}
