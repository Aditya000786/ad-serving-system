-- Atomic budget deduction with both total and daily budget checks
-- KEYS[1] = budget:{campaignId}:remaining (total budget)
-- KEYS[2] = budget:{campaignId}:daily_remaining (daily budget)
-- ARGV[1] = cost in cents

local totalRemaining = tonumber(redis.call('GET', KEYS[1]) or '0')
local dailyRemaining = tonumber(redis.call('GET', KEYS[2]) or '0')
local cost = tonumber(ARGV[1])

if totalRemaining < cost then
    return -1  -- INSUFFICIENT total budget
end

if dailyRemaining < cost then
    return -2  -- INSUFFICIENT daily budget
end

redis.call('DECRBY', KEYS[1], cost)
redis.call('DECRBY', KEYS[2], cost)

return 1  -- OK
