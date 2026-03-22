package com.adserving.core.port;

import com.adserving.core.model.Campaign;
import com.adserving.core.model.CampaignStatus;

import java.util.List;
import java.util.Optional;

public interface CampaignRepository {
    Optional<Campaign> findById(String id);
    List<Campaign> findByStatus(CampaignStatus status);
    Campaign save(Campaign campaign);
}
