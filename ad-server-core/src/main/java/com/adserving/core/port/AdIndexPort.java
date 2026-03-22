package com.adserving.core.port;

import com.adserving.core.model.Ad;
import com.adserving.core.model.AdRequest;
import com.adserving.core.model.TargetingCriteria;

import java.util.Set;

public interface AdIndexPort {
    Set<String> findEligibleAdIds(AdRequest request);
    void indexAd(Ad ad, TargetingCriteria criteria);
    void removeAd(String adId);
}
