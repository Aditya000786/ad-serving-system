package com.adserving.core.port;

import com.adserving.core.model.Ad;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdRepository {
    Optional<Ad> findById(String id);
    List<Ad> findByIds(Collection<String> ids);
    List<Ad> findByAdGroupId(String adGroupId);
    Ad save(Ad ad);
}
