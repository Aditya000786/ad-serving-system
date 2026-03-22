package com.adserving.core.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModelConstructionTest {

    @Test
    void shouldBuildCampaign() {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();

        Campaign campaign = Campaign.builder()
                .id(id)
                .name("Test Campaign")
                .advertiserId("adv-123")
                .dailyBudgetCents(100_00L)
                .totalBudgetCents(1000_00L)
                .status(CampaignStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(id, campaign.getId());
        assertEquals("Test Campaign", campaign.getName());
        assertEquals("adv-123", campaign.getAdvertiserId());
        assertEquals(10000L, campaign.getDailyBudgetCents());
        assertEquals(100000L, campaign.getTotalBudgetCents());
        assertEquals(CampaignStatus.ACTIVE, campaign.getStatus());
        assertEquals(LocalDate.of(2026, 1, 1), campaign.getStartDate());
        assertEquals(now, campaign.getCreatedAt());
    }

    @Test
    void shouldBuildAdGroup() {
        TargetingCriteria criteria = TargetingCriteria.builder()
                .devices(List.of("MOBILE"))
                .build();

        AdGroup adGroup = AdGroup.builder()
                .id(UUID.randomUUID().toString())
                .campaignId("campaign-1")
                .name("Mobile Users")
                .targetingCriteria(criteria)
                .bidAmountCents(150L)
                .bidType(BidType.CPC)
                .build();

        assertNotNull(adGroup.getId());
        assertEquals("campaign-1", adGroup.getCampaignId());
        assertEquals("Mobile Users", adGroup.getName());
        assertEquals(150L, adGroup.getBidAmountCents());
        assertEquals(BidType.CPC, adGroup.getBidType());
        assertEquals(List.of("MOBILE"), adGroup.getTargetingCriteria().getDevices());
    }

    @Test
    void shouldBuildAd() {
        Ad ad = Ad.builder()
                .id(UUID.randomUUID().toString())
                .adGroupId("ag-1")
                .title("Best Product Ever")
                .description("Buy now and save 50%")
                .creativeUrl("https://cdn.example.com/creative.png")
                .clickUrl("https://example.com/landing")
                .status(AdStatus.ACTIVE)
                .campaignId("campaign-1")
                .bidAmountCents(200L)
                .build();

        assertNotNull(ad.getId());
        assertEquals("ag-1", ad.getAdGroupId());
        assertEquals("Best Product Ever", ad.getTitle());
        assertEquals(AdStatus.ACTIVE, ad.getStatus());
        assertEquals("campaign-1", ad.getCampaignId());
        assertEquals(200L, ad.getBidAmountCents());
    }

    @Test
    void shouldBuildAdRequest() {
        AdRequest request = AdRequest.builder()
                .geo("US")
                .city("New York")
                .category("technology")
                .device("MOBILE")
                .userId("user-123")
                .build();

        assertEquals("US", request.getGeo());
        assertEquals("New York", request.getCity());
        assertEquals("technology", request.getCategory());
        assertEquals("MOBILE", request.getDevice());
        assertEquals("user-123", request.getUserId());
    }

    @Test
    void shouldBuildAdResponse() {
        AuctionInfo auctionInfo = AuctionInfo.builder()
                .winningBid(2.50)
                .pricePaid(2.00)
                .auctionType("SECOND_PRICE")
                .build();

        AdResponse response = AdResponse.builder()
                .adId("ad-1")
                .title("Great Ad")
                .creativeUrl("https://cdn.example.com/creative.png")
                .clickUrl("https://example.com/click")
                .impressionUrl("https://example.com/impression")
                .auction(auctionInfo)
                .build();

        assertEquals("ad-1", response.getAdId());
        assertEquals("Great Ad", response.getTitle());
        assertEquals("SECOND_PRICE", response.getAuction().getAuctionType());
        assertEquals(2.00, response.getAuction().getPricePaid());
    }

    @Test
    void shouldBuildAuctionResult() {
        Ad winner = Ad.builder()
                .id("ad-1")
                .title("Winner Ad")
                .bidAmountCents(300L)
                .build();

        AuctionResult result = AuctionResult.builder()
                .winner(winner)
                .clearingPrice(250.0)
                .candidateCount(5)
                .build();

        assertEquals("ad-1", result.getWinner().getId());
        assertEquals(250.0, result.getClearingPrice());
        assertEquals(5, result.getCandidateCount());
    }

    @Test
    void shouldEnumerateCampaignStatuses() {
        CampaignStatus[] statuses = CampaignStatus.values();
        assertEquals(5, statuses.length);
        assertEquals(CampaignStatus.DRAFT, CampaignStatus.valueOf("DRAFT"));
        assertEquals(CampaignStatus.ACTIVE, CampaignStatus.valueOf("ACTIVE"));
        assertEquals(CampaignStatus.PAUSED, CampaignStatus.valueOf("PAUSED"));
        assertEquals(CampaignStatus.ENDED, CampaignStatus.valueOf("ENDED"));
        assertEquals(CampaignStatus.BUDGET_EXHAUSTED, CampaignStatus.valueOf("BUDGET_EXHAUSTED"));
    }

    @Test
    void shouldEnumerateAdStatuses() {
        AdStatus[] statuses = AdStatus.values();
        assertEquals(3, statuses.length);
        assertEquals(AdStatus.ACTIVE, AdStatus.valueOf("ACTIVE"));
        assertEquals(AdStatus.PAUSED, AdStatus.valueOf("PAUSED"));
        assertEquals(AdStatus.ARCHIVED, AdStatus.valueOf("ARCHIVED"));
    }
}
