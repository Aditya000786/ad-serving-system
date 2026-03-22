package com.adserving.core.port;

public interface BudgetPort {
    boolean hasRemainingBudget(String campaignId);
    boolean deductBudget(String campaignId, long costCents);
    long getRemainingBudget(String campaignId);
    long getDailyRemainingBudget(String campaignId);
    void resetDailyBudget(String campaignId, long dailyBudgetCents);
    double getPacingMultiplier(String campaignId);
}
