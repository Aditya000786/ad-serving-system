package com.adserving.api.adapter;

import com.adserving.api.entity.AdEntity;
import com.adserving.api.entity.AdGroupEntity;
import com.adserving.core.model.*;
import com.adserving.core.port.AdRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.*;

@Component
public class JpaAdRepositoryImpl implements AdRepository {

    private final SpringAdRepository springAdRepo;
    private final SpringAdGroupRepository springAdGroupRepo;
    private final ObjectMapper objectMapper;

    public JpaAdRepositoryImpl(SpringAdRepository springAdRepo,
                               SpringAdGroupRepository springAdGroupRepo,
                               ObjectMapper objectMapper) {
        this.springAdRepo = springAdRepo;
        this.springAdGroupRepo = springAdGroupRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Ad> findById(String id) {
        return springAdRepo.findById(id).map(this::toDomain);
    }

    @Override
    @Cacheable(value = "adMetadata", key = "'batch:' + #ids.hashCode()")
    public List<Ad> findByIds(Collection<String> ids) {
        List<AdEntity> entities = springAdRepo.findAllById(ids);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public List<Ad> findByAdGroupId(String adGroupId) {
        return springAdRepo.findByAdGroupId(adGroupId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public Ad save(Ad ad) {
        AdEntity entity = toEntity(ad);
        return toDomain(springAdRepo.save(entity));
    }

    private Ad toDomain(AdEntity entity) {
        // Fetch ad group to get campaign_id, bid, targeting
        AdGroupEntity adGroup = springAdGroupRepo.findById(entity.getAdGroupId()).orElse(null);

        Ad.AdBuilder builder = Ad.builder()
                .id(entity.getId())
                .adGroupId(entity.getAdGroupId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .creativeUrl(entity.getCreativeUrl())
                .clickUrl(entity.getClickUrl())
                .status(AdStatus.valueOf(entity.getStatus()));

        if (adGroup != null) {
            builder.campaignId(adGroup.getCampaignId())
                    .bidAmountCents(adGroup.getBidAmountCents())
                    .targetingCriteria(parseTargetingCriteria(adGroup.getTargetingCriteria()));
        }

        return builder.build();
    }

    private AdEntity toEntity(Ad ad) {
        return AdEntity.builder()
                .id(ad.getId() != null ? ad.getId() : UUID.randomUUID().toString())
                .adGroupId(ad.getAdGroupId())
                .title(ad.getTitle())
                .description(ad.getDescription())
                .creativeUrl(ad.getCreativeUrl())
                .clickUrl(ad.getClickUrl())
                .status(ad.getStatus() != null ? ad.getStatus().name() : "ACTIVE")
                .build();
    }

    private TargetingCriteria parseTargetingCriteria(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) {
            return null;
        }
        try {
            return objectMapper.readValue(json, TargetingCriteria.class);
        } catch (Exception e) {
            return null;
        }
    }
}

@Repository
interface SpringAdRepository extends JpaRepository<AdEntity, String> {
    List<AdEntity> findByAdGroupId(String adGroupId);
}

@Repository
interface SpringAdGroupRepository extends JpaRepository<AdGroupEntity, String> {
    List<AdGroupEntity> findByCampaignId(String campaignId);
}

@Repository
interface SpringCampaignRepository extends JpaRepository<com.adserving.api.entity.CampaignEntity, String> {
    List<com.adserving.api.entity.CampaignEntity> findByStatus(String status);
}
