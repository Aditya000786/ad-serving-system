package com.adserving.core.auction;

import com.adserving.core.model.Ad;
import com.adserving.core.model.AdRequest;

public interface AdRanker {
    double score(Ad ad, AdRequest request);
}
