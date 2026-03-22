package com.adserving.api.service;

import com.adserving.core.auction.AdRanker;
import com.adserving.core.model.Ad;
import com.adserving.core.model.AdRequest;
import org.springframework.stereotype.Component;

@Component
public class SimpleAdRanker implements AdRanker {

    private static final double UNIFORM_CTR = 0.01;

    @Override
    public double score(Ad ad, AdRequest request) {
        return ad.getBidAmountCents() * UNIFORM_CTR;
    }
}
