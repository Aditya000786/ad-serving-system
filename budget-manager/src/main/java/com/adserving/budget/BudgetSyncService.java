package com.adserving.budget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class BudgetSyncService {

    private static final Logger log = LoggerFactory.getLogger(BudgetSyncService.class);

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbcTemplate;

    public BudgetSyncService(StringRedisTemplate redis, JdbcTemplate jdbcTemplate) {
        this.redis = redis;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedRate = 30_000) // every 30 seconds
    public void syncBudgetsToPostgres() {
        Set<String> campaignKeys = redis.keys("budget:*:remaining");
        if (campaignKeys == null || campaignKeys.isEmpty()) {
            return;
        }

        int synced = 0;
        for (String key : campaignKeys) {
            String campaignId = key.replace("budget:", "").replace(":remaining", "");
            try {
                syncCampaignBudget(campaignId);
                synced++;
            } catch (Exception e) {
                log.error("Failed to sync budget for campaign {}", campaignId, e);
            }
        }

        log.debug("Synced {} campaign budgets to PostgreSQL", synced);
    }

    private void syncCampaignBudget(String campaignId) {
        String totalVal = redis.opsForValue().get("budget:" + campaignId + ":remaining");
        if (totalVal == null) {
            return;
        }
        long remainingCents = Long.parseLong(totalVal);

        // Update the total_budget_cents to reflect remaining budget
        // Note: we store remaining, not spent. The original total is in the campaigns table.
        jdbcTemplate.update(
                "UPDATE campaigns SET total_budget_cents = ?, updated_at = NOW() WHERE id = ?",
                remainingCents, campaignId);
    }
}
