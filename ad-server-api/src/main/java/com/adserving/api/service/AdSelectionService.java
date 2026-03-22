package com.adserving.api.service;

import com.adserving.core.auction.AdRanker;
import com.adserving.core.model.*;
import com.adserving.core.port.AdIndexPort;
import com.adserving.core.port.AdRepository;
import com.adserving.targeting.TargetingEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AdSelectionService {

    private static final Logger log = LoggerFactory.getLogger(AdSelectionService.class);

    private final AdIndexPort adIndex;
    private final AdRepository adRepository;
    private final TargetingEvaluator targetingEvaluator;
    private final AdRanker adRanker;
    private final TrackTokenService trackTokenService;

    public AdSelectionService(AdIndexPort adIndex,
                              AdRepository adRepository,
                              TargetingEvaluator targetingEvaluator,
                              AdRanker adRanker,
                              TrackTokenService trackTokenService) {
        this.adIndex = adIndex;
        this.adRepository = adRepository;
        this.targetingEvaluator = targetingEvaluator;
        this.adRanker = adRanker;
        this.trackTokenService = trackTokenService;
    }

    public Optional<AdResponse> selectAd(AdRequest request) {
        // 1. Query Redis for eligible ad IDs
        Set<String> eligibleIds = adIndex.findEligibleAdIds(request);
        if (eligibleIds.isEmpty()) {
            log.debug("No eligible ads found in index for request: {}", request);
            return Optional.empty();
        }

        // 2. Fetch ad metadata
        List<Ad> candidates = adRepository.findByIds(eligibleIds);
        if (candidates.isEmpty()) {
            log.debug("No ad metadata found for eligible IDs: {}", eligibleIds);
            return Optional.empty();
        }

        // 3. Double-check targeting (Redis index may be slightly stale)
        List<Ad> targeted = candidates.stream()
                .filter(ad -> ad.getTargetingCriteria() == null
                        || targetingEvaluator.isEligible(ad.getTargetingCriteria(), request))
                .filter(ad -> ad.getStatus() == AdStatus.ACTIVE)
                .toList();

        if (targeted.isEmpty()) {
            log.debug("No ads passed targeting evaluation");
            return Optional.empty();
        }

        // 4. Score and rank
        record ScoredAd(Ad ad, double score) {}
        List<ScoredAd> scored = targeted.stream()
                .map(ad -> new ScoredAd(ad, adRanker.score(ad, request)))
                .sorted(Comparator.comparingDouble(ScoredAd::score).reversed())
                .toList();

        ScoredAd winner = scored.getFirst();
        Ad winnerAd = winner.ad();

        // 5. Calculate price (simplified for Phase 1 — full GSP in Phase 3)
        double pricePaidCents;
        if (scored.size() > 1) {
            pricePaidCents = scored.get(1).score() / 0.01 + 1; // second price + $0.01
        } else {
            pricePaidCents = winnerAd.getBidAmountCents() * 0.5; // floor price
        }

        // 6. Generate track token
        String trackToken = trackTokenService.generateToken(
                winnerAd.getId(),
                winnerAd.getCampaignId(),
                request.getUserId(),
                request.getGeo());

        // 7. Build response
        AdResponse response = AdResponse.builder()
                .adId(winnerAd.getId())
                .title(winnerAd.getTitle())
                .creativeUrl(winnerAd.getCreativeUrl())
                .clickUrl("/v1/click?ad=" + winnerAd.getId() + "&track=" + trackToken)
                .impressionUrl("/v1/impression?ad=" + winnerAd.getId() + "&track=" + trackToken)
                .auction(AuctionInfo.builder()
                        .winningBid(winnerAd.getBidAmountCents() / 100.0)
                        .pricePaid(pricePaidCents / 100.0)
                        .auctionType("SECOND_PRICE")
                        .build())
                .build();

        return Optional.of(response);
    }
}
