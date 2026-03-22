package com.adserving.api.service;

import com.adserving.core.auction.AdRanker;
import com.adserving.core.model.*;
import com.adserving.core.port.AdIndexPort;
import com.adserving.core.port.AdRepository;
import com.adserving.core.port.BudgetPort;
import com.adserving.core.port.EventPort;
import com.adserving.targeting.TargetingEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class AdSelectionService {

    private static final Logger log = LoggerFactory.getLogger(AdSelectionService.class);

    private final AdIndexPort adIndex;
    private final AdRepository adRepository;
    private final TargetingEvaluator targetingEvaluator;
    private final AdRanker adRanker;
    private final TrackTokenService trackTokenService;
    private final BudgetPort budgetPort;
    private final EventPort eventPort;

    public AdSelectionService(AdIndexPort adIndex,
                              AdRepository adRepository,
                              TargetingEvaluator targetingEvaluator,
                              AdRanker adRanker,
                              TrackTokenService trackTokenService,
                              BudgetPort budgetPort,
                              EventPort eventPort) {
        this.adIndex = adIndex;
        this.adRepository = adRepository;
        this.targetingEvaluator = targetingEvaluator;
        this.adRanker = adRanker;
        this.trackTokenService = trackTokenService;
        this.budgetPort = budgetPort;
        this.eventPort = eventPort;
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

        // 4. Filter by budget availability
        List<Ad> withBudget = targeted.stream()
                .filter(ad -> budgetPort.hasRemainingBudget(ad.getCampaignId()))
                .toList();

        if (withBudget.isEmpty()) {
            log.debug("All eligible ads have exhausted budgets");
            return Optional.empty();
        }

        // 5. Apply pacing — probabilistic throttle based on pacing multiplier
        List<Ad> paced = new ArrayList<>();
        Random rand = new Random();
        for (Ad ad : withBudget) {
            double multiplier = budgetPort.getPacingMultiplier(ad.getCampaignId());
            if (multiplier >= 1.0 || rand.nextDouble() < multiplier) {
                paced.add(ad);
            }
        }

        if (paced.isEmpty()) {
            // If pacing filtered all, fall back to withBudget to avoid zero-serving
            paced = withBudget;
        }

        // 6. Score and rank
        record ScoredAd(Ad ad, double score) {}
        List<ScoredAd> scored = paced.stream()
                .map(ad -> new ScoredAd(ad, adRanker.score(ad, request)))
                .sorted(Comparator.comparingDouble(ScoredAd::score).reversed())
                .toList();

        ScoredAd winner = scored.getFirst();
        Ad winnerAd = winner.ad();

        // 7. Calculate price (GSP second-price)
        double pricePaidCents;
        if (scored.size() > 1) {
            pricePaidCents = scored.get(1).score() / 0.01 + 1; // second price + $0.01
        } else {
            pricePaidCents = winnerAd.getBidAmountCents() * 0.5; // floor price
        }

        // 8. Atomic budget deduction (charge at serve time)
        long costCents = Math.round(pricePaidCents);
        if (!budgetPort.deductBudget(winnerAd.getCampaignId(), costCents)) {
            log.debug("Budget deduction failed for campaign {}", winnerAd.getCampaignId());
            return Optional.empty();
        }

        // 9. Generate track token
        String trackToken = trackTokenService.generateToken(
                winnerAd.getId(),
                winnerAd.getCampaignId(),
                request.getUserId(),
                request.getGeo());

        // 10. Publish impression event (async, fire-and-forget)
        eventPort.publishEvent(AdEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .type(EventType.IMPRESSION)
                .adId(winnerAd.getId())
                .campaignId(winnerAd.getCampaignId())
                .userId(request.getUserId())
                .timestamp(Instant.now())
                .context(EventContext.builder()
                        .geo(request.getGeo())
                        .device(request.getDevice())
                        .build())
                .build());

        // 11. Build response
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
