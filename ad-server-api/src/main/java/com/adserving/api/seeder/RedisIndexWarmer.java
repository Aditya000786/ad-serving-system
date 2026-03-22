package com.adserving.api.seeder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@Order(2)
public class RedisIndexWarmer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RedisIndexWarmer.class);

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisIndexWarmer(JdbcTemplate jdbc, StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        log.info("Warming Redis ad index...");

        String query = """
                SELECT a.id AS ad_id, ag.targeting_criteria
                FROM ads a
                JOIN ad_groups ag ON a.ad_group_id = ag.id
                JOIN campaigns c ON ag.campaign_id = c.id
                WHERE a.status = 'ACTIVE' AND c.status = 'ACTIVE'
                """;

        List<Map<String, Object>> rows = jdbc.queryForList(query);
        int indexed = 0;

        for (Map<String, Object> row : rows) {
            String adId = (String) row.get("ad_id");
            String criteriaJson = row.get("targeting_criteria").toString();

            try {
                JsonNode criteria = objectMapper.readTree(criteriaJson);

                // Index geo countries
                JsonNode geo = criteria.get("geo");
                if (geo != null) {
                    JsonNode countries = geo.get("countries");
                    if (countries != null && countries.isArray()) {
                        for (JsonNode country : countries) {
                            redis.opsForSet().add("geo:" + country.asText().toUpperCase(), adId);
                        }
                    }
                }

                // Index categories
                JsonNode categories = criteria.get("categories");
                if (categories != null && categories.isArray()) {
                    for (JsonNode cat : categories) {
                        redis.opsForSet().add("category:" + cat.asText().toLowerCase(), adId);
                    }
                }

                // Index devices
                JsonNode devices = criteria.get("devices");
                if (devices != null && devices.isArray()) {
                    for (JsonNode dev : devices) {
                        redis.opsForSet().add("device:" + dev.asText().toUpperCase(), adId);
                    }
                }

                indexed++;
            } catch (Exception e) {
                log.warn("Failed to index ad {}: {}", adId, e.getMessage());
            }
        }

        log.info("Indexed {} ads into Redis", indexed);
    }
}
