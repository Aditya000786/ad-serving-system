package com.adserving.budget;

import com.adserving.core.port.BudgetPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Component
public class RedisBudgetManager implements BudgetPort {

    private static final Logger log = LoggerFactory.getLogger(RedisBudgetManager.class);

    private final StringRedisTemplate redis;
    private DefaultRedisScript<Long> deductScript;

    public RedisBudgetManager(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @PostConstruct
    public void init() {
        deductScript = new DefaultRedisScript<>();
        deductScript.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/deduct_budget.lua")));
        deductScript.setResultType(Long.class);
    }

    @Override
    public boolean hasRemainingBudget(String campaignId) {
        long total = getRemainingBudget(campaignId);
        long daily = getDailyRemainingBudget(campaignId);
        return total > 0 && daily > 0;
    }

    @Override
    public boolean deductBudget(String campaignId, long costCents) {
        String totalKey = "budget:" + campaignId + ":remaining";
        String dailyKey = "budget:" + campaignId + ":daily_remaining";

        Long result = redis.execute(deductScript, List.of(totalKey, dailyKey),
                String.valueOf(costCents));

        if (result == null || result < 0) {
            log.debug("Budget deduction failed for campaign {}: result={}", campaignId, result);
            return false;
        }
        return true;
    }

    @Override
    public long getRemainingBudget(String campaignId) {
        String val = redis.opsForValue().get("budget:" + campaignId + ":remaining");
        return val != null ? Long.parseLong(val) : 0;
    }

    @Override
    public long getDailyRemainingBudget(String campaignId) {
        String val = redis.opsForValue().get("budget:" + campaignId + ":daily_remaining");
        return val != null ? Long.parseLong(val) : 0;
    }

    @Override
    public void resetDailyBudget(String campaignId, long dailyBudgetCents) {
        redis.opsForValue().set("budget:" + campaignId + ":daily_remaining",
                String.valueOf(dailyBudgetCents));
        log.info("Reset daily budget for campaign {} to {} cents", campaignId, dailyBudgetCents);
    }

    @Override
    public double getPacingMultiplier(String campaignId) {
        String val = redis.opsForValue().get("pacing:" + campaignId + ":multiplier");
        return val != null ? Double.parseDouble(val) : 1.0;
    }

    public void initializeBudget(String campaignId, long totalBudgetCents, long dailyBudgetCents) {
        redis.opsForValue().set("budget:" + campaignId + ":remaining",
                String.valueOf(totalBudgetCents));
        redis.opsForValue().set("budget:" + campaignId + ":daily_remaining",
                String.valueOf(dailyBudgetCents));
    }
}
