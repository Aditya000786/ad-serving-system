package com.adserving.api.seeder;

import com.adserving.budget.RedisBudgetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Order(3)
public class BudgetWarmer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BudgetWarmer.class);

    private final JdbcTemplate jdbc;
    private final RedisBudgetManager budgetManager;
    private final StringRedisTemplate redis;

    public BudgetWarmer(JdbcTemplate jdbc, RedisBudgetManager budgetManager, StringRedisTemplate redis) {
        this.jdbc = jdbc;
        this.budgetManager = budgetManager;
        this.redis = redis;
    }

    @Override
    public void run(String... args) {
        log.info("Warming budget data in Redis...");

        List<Map<String, Object>> campaigns = jdbc.queryForList(
                "SELECT id, daily_budget_cents, total_budget_cents FROM campaigns WHERE status = 'ACTIVE'");

        int count = 0;
        for (Map<String, Object> campaign : campaigns) {
            String id = (String) campaign.get("id");
            long totalBudget = ((Number) campaign.get("total_budget_cents")).longValue();
            long dailyBudget = ((Number) campaign.get("daily_budget_cents")).longValue();

            budgetManager.initializeBudget(id, totalBudget, dailyBudget);
            // Store original daily budget for pacing calculations
            redis.opsForValue().set("budget:" + id + ":daily_budget", String.valueOf(dailyBudget));
            count++;
        }

        log.info("Initialized budgets for {} campaigns in Redis", count);
    }
}
