package com.adserving.core.port;

import com.adserving.core.model.AdGroup;

import java.util.List;
import java.util.Optional;

public interface AdGroupRepository {
    Optional<AdGroup> findById(String id);
    List<AdGroup> findByCampaignId(String campaignId);
    AdGroup save(AdGroup adGroup);
}
