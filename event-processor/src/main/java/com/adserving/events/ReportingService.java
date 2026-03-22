package com.adserving.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReportingService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneOffset.UTC);

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbcTemplate;

    public ReportingService(StringRedisTemplate redis, JdbcTemplate jdbcTemplate) {
        this.redis = redis;
        this.jdbcTemplate = jdbcTemplate;
    }

    public CampaignStats getRealTimeStats(String campaignId) {
        String dateKey = DATE_FMT.format(Instant.now());
        // Aggregate across all ads for this campaign from Redis
        var keys = redis.keys("stats:" + campaignId + ":*:daily:" + dateKey);
        long totalImpressions = 0, totalClicks = 0, totalConversions = 0;

        if (keys != null) {
            for (String key : keys) {
                var counters = redis.opsForHash().entries(key);
                totalImpressions += parseLong(counters.get("impressions"));
                totalClicks += parseLong(counters.get("clicks"));
                totalConversions += parseLong(counters.get("conversions"));
            }
        }

        double ctr = totalImpressions > 0 ? (double) totalClicks / totalImpressions : 0;

        return CampaignStats.builder()
                .campaignId(campaignId)
                .date(dateKey)
                .impressions(totalImpressions)
                .clicks(totalClicks)
                .conversions(totalConversions)
                .ctr(ctr)
                .build();
    }

    public List<CampaignStats> getHistoricalStats(String campaignId, String from, String to, String granularity) {
        if ("HOURLY".equalsIgnoreCase(granularity)) {
            return getHourlyStats(campaignId, from, to);
        }
        return getDailyStats(campaignId, from, to);
    }

    private List<CampaignStats> getHourlyStats(String campaignId, String from, String to) {
        var rows = jdbcTemplate.queryForList("""
                SELECT hour, SUM(impressions) as impressions, SUM(clicks) as clicks,
                       SUM(conversions) as conversions, SUM(spend) as spend
                FROM hourly_stats
                WHERE campaign_id = ? AND hour >= ?::timestamp AND hour <= ?::timestamp
                GROUP BY hour ORDER BY hour
                """, campaignId, from + " 00:00:00", to + " 23:59:59");

        return mapToStats(campaignId, rows, "hour");
    }

    private List<CampaignStats> getDailyStats(String campaignId, String from, String to) {
        var rows = jdbcTemplate.queryForList("""
                SELECT date, SUM(impressions) as impressions, SUM(clicks) as clicks,
                       SUM(conversions) as conversions, SUM(spend) as spend,
                       AVG(avg_cpc) as avg_cpc, AVG(ctr) as ctr
                FROM daily_stats
                WHERE campaign_id = ? AND date >= ?::date AND date <= ?::date
                GROUP BY date ORDER BY date
                """, campaignId, from, to);

        return mapToStats(campaignId, rows, "date");
    }

    private List<CampaignStats> mapToStats(String campaignId, List<Map<String, Object>> rows, String timeCol) {
        List<CampaignStats> result = new ArrayList<>();
        for (var row : rows) {
            long impressions = ((Number) row.get("impressions")).longValue();
            long clicks = ((Number) row.get("clicks")).longValue();
            long conversions = ((Number) row.get("conversions")).longValue();
            double ctr = impressions > 0 ? (double) clicks / impressions : 0;

            result.add(CampaignStats.builder()
                    .campaignId(campaignId)
                    .date(row.get(timeCol).toString())
                    .impressions(impressions)
                    .clicks(clicks)
                    .conversions(conversions)
                    .ctr(ctr)
                    .spend(((Number) row.get("spend")).longValue())
                    .build());
        }
        return result;
    }

    private long parseLong(Object val) {
        if (val == null) return 0;
        return Long.parseLong(val.toString());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignStats {
        private String campaignId;
        private String date;
        private long impressions;
        private long clicks;
        private long conversions;
        private double ctr;
        private long spend;
    }
}
