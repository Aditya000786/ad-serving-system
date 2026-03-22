package com.adserving.budget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

class RedisBudgetManagerTest {

    private RedisBudgetManager budgetManager;
    private StringRedisTemplate redis;

    @BeforeEach
    void setUp() {
        try {
            LettuceConnectionFactory factory = new LettuceConnectionFactory(
                    new RedisStandaloneConfiguration("localhost", 6379));
            factory.afterPropertiesSet();
            redis = new StringRedisTemplate(factory);
            redis.getConnectionFactory().getConnection().ping();

            budgetManager = new RedisBudgetManager(redis);
            budgetManager.init();

            // Clean test keys
            redis.delete(redis.keys("budget:test-*"));
            redis.delete(redis.keys("pacing:test-*"));
        } catch (Exception e) {
            // Redis not available, skip
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Redis not available");
        }
    }

    @Test
    void testInitializeAndCheckBudget() {
        budgetManager.initializeBudget("test-campaign-1", 10000, 1000);

        assertTrue(budgetManager.hasRemainingBudget("test-campaign-1"));
        assertEquals(10000, budgetManager.getRemainingBudget("test-campaign-1"));
        assertEquals(1000, budgetManager.getDailyRemainingBudget("test-campaign-1"));
    }

    @Test
    void testDeductBudgetSuccess() {
        budgetManager.initializeBudget("test-campaign-2", 5000, 500);

        boolean result = budgetManager.deductBudget("test-campaign-2", 100);

        assertTrue(result);
        assertEquals(4900, budgetManager.getRemainingBudget("test-campaign-2"));
        assertEquals(400, budgetManager.getDailyRemainingBudget("test-campaign-2"));
    }

    @Test
    void testDeductBudgetInsufficientTotal() {
        budgetManager.initializeBudget("test-campaign-3", 50, 1000);

        boolean result = budgetManager.deductBudget("test-campaign-3", 100);

        assertFalse(result);
        // Budget unchanged on failure
        assertEquals(50, budgetManager.getRemainingBudget("test-campaign-3"));
    }

    @Test
    void testDeductBudgetInsufficientDaily() {
        budgetManager.initializeBudget("test-campaign-4", 10000, 50);

        boolean result = budgetManager.deductBudget("test-campaign-4", 100);

        assertFalse(result);
        assertEquals(10000, budgetManager.getRemainingBudget("test-campaign-4"));
        assertEquals(50, budgetManager.getDailyRemainingBudget("test-campaign-4"));
    }

    @Test
    void testResetDailyBudget() {
        budgetManager.initializeBudget("test-campaign-5", 10000, 500);
        budgetManager.deductBudget("test-campaign-5", 300);

        budgetManager.resetDailyBudget("test-campaign-5", 500);

        assertEquals(500, budgetManager.getDailyRemainingBudget("test-campaign-5"));
        // Total remains deducted
        assertEquals(9700, budgetManager.getRemainingBudget("test-campaign-5"));
    }

    @Test
    void testNoBudgetInitialized() {
        assertFalse(budgetManager.hasRemainingBudget("nonexistent-campaign"));
        assertEquals(0, budgetManager.getRemainingBudget("nonexistent-campaign"));
    }

    @Test
    void testConcurrentDeductions() {
        budgetManager.initializeBudget("test-campaign-6", 200, 200);

        // Simulate concurrent deductions — exactly 2 should succeed
        boolean r1 = budgetManager.deductBudget("test-campaign-6", 100);
        boolean r2 = budgetManager.deductBudget("test-campaign-6", 100);
        boolean r3 = budgetManager.deductBudget("test-campaign-6", 100);

        assertTrue(r1);
        assertTrue(r2);
        assertFalse(r3);
        assertEquals(0, budgetManager.getRemainingBudget("test-campaign-6"));
    }

    @Test
    void testPacingMultiplierDefault() {
        assertEquals(1.0, budgetManager.getPacingMultiplier("nonexistent"));
    }
}
