package com.adserving.api.adapter;

import com.adserving.core.model.Ad;
import com.adserving.core.model.AdRequest;
import com.adserving.core.model.GeoTargeting;
import com.adserving.core.model.TargetingCriteria;
import com.adserving.core.port.AdIndexPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RedisAdIndex implements AdIndexPort {

    private static final Logger log = LoggerFactory.getLogger(RedisAdIndex.class);
    private static final long MAX_SET_CARDINALITY = 5000;

    private final StringRedisTemplate redis;

    public RedisAdIndex(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Set<String> findEligibleAdIds(AdRequest request) {
        List<String> keys = new ArrayList<>();

        if (request.getGeo() != null) {
            keys.add("geo:" + request.getGeo().toUpperCase());
        }
        if (request.getCategory() != null) {
            keys.add("category:" + request.getCategory().toLowerCase());
        }
        if (request.getDevice() != null) {
            keys.add("device:" + request.getDevice().toUpperCase());
        }

        if (keys.isEmpty()) {
            return Collections.emptySet();
        }

        // If only one key, return its members directly
        if (keys.size() == 1) {
            Set<String> members = redis.opsForSet().members(keys.getFirst());
            return members != null ? members : Collections.emptySet();
        }

        // SINTER across all targeting dimension keys
        Set<String> result = redis.opsForSet().intersect(keys);
        return result != null ? result : Collections.emptySet();
    }

    @Override
    public void indexAd(Ad ad, TargetingCriteria criteria) {
        if (criteria == null) {
            return;
        }

        String adId = ad.getId();

        // Index geo
        if (criteria.getGeo() != null) {
            GeoTargeting geo = criteria.getGeo();
            if (geo.getCountries() != null) {
                for (String country : geo.getCountries()) {
                    addToSetWithCap("geo:" + country.toUpperCase(), adId);
                }
            }
            if (geo.getCities() != null) {
                for (String city : geo.getCities()) {
                    addToSetWithCap("geo:city:" + city.toLowerCase(), adId);
                }
            }
        }

        // Index categories
        if (criteria.getCategories() != null) {
            for (String category : criteria.getCategories()) {
                addToSetWithCap("category:" + category.toLowerCase(), adId);
            }
        }

        // Index devices
        if (criteria.getDevices() != null) {
            for (String device : criteria.getDevices()) {
                addToSetWithCap("device:" + device.toUpperCase(), adId);
            }
        }
    }

    @Override
    public void removeAd(String adId) {
        // Scan known key patterns and remove
        Set<String> keys = redis.keys("geo:*");
        removeFromKeys(keys, adId);
        keys = redis.keys("category:*");
        removeFromKeys(keys, adId);
        keys = redis.keys("device:*");
        removeFromKeys(keys, adId);
    }

    private void addToSetWithCap(String key, String adId) {
        Long size = redis.opsForSet().size(key);
        if (size != null && size >= MAX_SET_CARDINALITY) {
            log.warn("Set {} has reached cardinality cap of {}", key, MAX_SET_CARDINALITY);
            return;
        }
        redis.opsForSet().add(key, adId);
    }

    private void removeFromKeys(Set<String> keys, String adId) {
        if (keys != null) {
            for (String key : keys) {
                redis.opsForSet().remove(key, adId);
            }
        }
    }
}
