package com.adserving.api.seeder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Random random = new Random(42);

    private static final String[] COUNTRIES = {"US", "IN", "UK", "DE", "FR", "JP", "BR", "CA", "AU", "SG"};
    private static final String[] CITIES = {"San Francisco", "New York", "London", "Berlin", "Mumbai", "Tokyo", "São Paulo", "Toronto", "Sydney", "Singapore"};
    private static final String[] CATEGORIES = {"technology", "finance", "health", "sports", "entertainment", "travel", "food", "education", "automotive", "fashion"};
    private static final String[] DEVICES_OPTIONS = {"MOBILE", "DESKTOP", "TABLET"};
    private static final String[] ADVERTISERS = {
            "TechCorp", "FinanceHub", "HealthPlus", "SportZone", "EntertainNow",
            "TravelGo", "FoodieApp", "EduLearn", "AutoDrive", "FashionBuzz",
            "CloudScale", "DataFlow", "AIWorks", "CyberShield", "GreenEnergy",
            "FitLife", "MusicStream", "GameVerse", "BookWorm", "PetCare",
            "HomeDecor", "BeautyBox", "CodeAcademy", "JobConnect", "NewsFlash",
            "CryptoTrade", "MedTech", "FarmFresh", "RideShare", "SmartHome",
            "VRWorld", "DroneHub", "SpaceTech", "BioLab", "LegalEase",
            "InsureSafe", "WealthPlan", "FoodDelivery", "StreamBox", "PhotoSnap",
            "TravelLux", "EcoStore", "RoboTech", "SolarPanel", "WindPower",
            "UrbanFarm", "PetAdopt", "KidZone", "SeniorCare", "ArtGallery"
    };

    public DataSeeder(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM campaigns", Integer.class);
        if (count != null && count > 0) {
            log.info("Database already seeded with {} campaigns, skipping", count);
            return;
        }

        log.info("Seeding database with demo data...");

        int campaignCount = 50;
        int adGroupCount = 0;
        int adCount = 0;

        // Status distribution: 40 ACTIVE, 5 PAUSED, 3 DRAFT, 2 ENDED
        String[] statuses = new String[50];
        Arrays.fill(statuses, 0, 40, "ACTIVE");
        Arrays.fill(statuses, 40, 45, "PAUSED");
        Arrays.fill(statuses, 45, 48, "DRAFT");
        Arrays.fill(statuses, 48, 50, "ENDED");

        for (int c = 0; c < campaignCount; c++) {
            String campaignId = UUID.nameUUIDFromBytes(("campaign-" + c).getBytes()).toString();
            long dailyBudget = 1000 + random.nextInt(49000);    // 1000-50000 cents
            long totalBudget = dailyBudget * (10 + random.nextInt(20)); // 10-30x daily
            LocalDate startDate = LocalDate.of(2026, 1, 1).plusDays(random.nextInt(60));
            LocalDate endDate = startDate.plusDays(30 + random.nextInt(60));

            jdbc.update(
                    "INSERT INTO campaigns (id, name, advertiser_id, daily_budget_cents, total_budget_cents, status, start_date, end_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    campaignId,
                    ADVERTISERS[c] + " Campaign",
                    UUID.nameUUIDFromBytes(("advertiser-" + c).getBytes()).toString(),
                    dailyBudget,
                    totalBudget,
                    statuses[c],
                    startDate,
                    endDate
            );

            // 4 ad groups per campaign
            for (int g = 0; g < 4; g++) {
                String adGroupId = UUID.nameUUIDFromBytes(("adgroup-" + c + "-" + g).getBytes()).toString();
                long bidAmount = 10 + random.nextInt(490); // 10-500 cents
                String targetingJson = generateTargetingCriteria(c, g);

                jdbc.update(
                        "INSERT INTO ad_groups (id, campaign_id, name, targeting_criteria, bid_amount_cents, bid_type) VALUES (?, ?, ?, ?::jsonb, ?, ?)",
                        adGroupId,
                        campaignId,
                        ADVERTISERS[c] + " AdGroup " + (g + 1),
                        targetingJson,
                        bidAmount,
                        "CPC"
                );
                adGroupCount++;

                // 2-3 ads per ad group (total ~500)
                int adsInGroup = (c * 4 + g) % 3 == 0 ? 3 : 2;
                for (int a = 0; a < adsInGroup; a++) {
                    String adId = UUID.nameUUIDFromBytes(("ad-" + c + "-" + g + "-" + a).getBytes()).toString();
                    String adStatus = statuses[c].equals("ACTIVE") ? "ACTIVE" : "PAUSED";

                    jdbc.update(
                            "INSERT INTO ads (id, ad_group_id, title, description, creative_url, click_url, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            adId,
                            adGroupId,
                            generateAdTitle(c, g, a),
                            generateAdDescription(c, g),
                            "https://cdn.example.com/creatives/" + adId + ".png",
                            "https://" + ADVERTISERS[c].toLowerCase().replace(" ", "") + ".example.com/landing?ref=adserve&ad=" + adId,
                            adStatus
                    );
                    adCount++;
                }
            }
        }

        log.info("Seeded {} campaigns, {} ad groups, {} ads", campaignCount, adGroupCount, adCount);
    }

    private String generateTargetingCriteria(int campaignIdx, int groupIdx) {
        try {
            ObjectNode criteria = objectMapper.createObjectNode();

            // Geo targeting
            ObjectNode geo = objectMapper.createObjectNode();
            ArrayNode countries = objectMapper.createArrayNode();
            int numCountries = 1 + random.nextInt(3);
            Set<String> picked = new HashSet<>();
            for (int i = 0; i < numCountries; i++) {
                String country = COUNTRIES[(campaignIdx + groupIdx + i) % COUNTRIES.length];
                if (picked.add(country)) countries.add(country);
            }
            geo.set("countries", countries);

            if (random.nextBoolean()) {
                ArrayNode cities = objectMapper.createArrayNode();
                cities.add(CITIES[(campaignIdx + groupIdx) % CITIES.length]);
                geo.set("cities", cities);
            }
            criteria.set("geo", geo);

            // Device targeting
            ArrayNode devices = objectMapper.createArrayNode();
            int numDevices = 1 + random.nextInt(DEVICES_OPTIONS.length);
            Set<String> pickedDevices = new HashSet<>();
            for (int i = 0; i < numDevices; i++) {
                String dev = DEVICES_OPTIONS[(groupIdx + i) % DEVICES_OPTIONS.length];
                if (pickedDevices.add(dev)) devices.add(dev);
            }
            criteria.set("devices", devices);

            // Category targeting
            ArrayNode categories = objectMapper.createArrayNode();
            int numCategories = 1 + random.nextInt(3);
            Set<String> pickedCats = new HashSet<>();
            for (int i = 0; i < numCategories; i++) {
                String cat = CATEGORIES[(campaignIdx + i) % CATEGORIES.length];
                if (pickedCats.add(cat)) categories.add(cat);
            }
            criteria.set("categories", categories);

            // Dayparting (50% of ad groups)
            if (groupIdx % 2 == 0) {
                ObjectNode dayparting = objectMapper.createObjectNode();
                ArrayNode hours = objectMapper.createArrayNode();
                ArrayNode days = objectMapper.createArrayNode();

                if (groupIdx == 0) {
                    // Business hours
                    for (int h = 9; h <= 17; h++) hours.add(h);
                    for (String d : new String[]{"MON", "TUE", "WED", "THU", "FRI"}) days.add(d);
                } else {
                    // Evening hours
                    for (int h = 18; h <= 23; h++) hours.add(h);
                    for (String d : new String[]{"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"}) days.add(d);
                }
                dayparting.set("hours", hours);
                dayparting.set("days", days);
                dayparting.put("timezone", "America/Los_Angeles");
                criteria.set("dayparting", dayparting);
            }

            return objectMapper.writeValueAsString(criteria);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String generateAdTitle(int c, int g, int a) {
        String[][] titles = {
                {"Discover", "Try", "Experience", "Unlock", "Get Started with"},
                {"the Best", "Amazing", "Top-Rated", "Premium", "Next-Gen"},
                {ADVERTISERS[c % ADVERTISERS.length] + " Solutions", "Tools", "Platform", "Services", "Products"}
        };
        return titles[0][(c + a) % titles[0].length] + " " +
                titles[1][(g + a) % titles[1].length] + " " +
                titles[2][(c + g) % titles[2].length];
    }

    private String generateAdDescription(int c, int g) {
        String[] descriptions = {
                "Transform your business with cutting-edge solutions. Start your free trial today.",
                "Join millions of satisfied customers. Limited time offer - save 30%.",
                "Industry-leading performance and reliability. See why experts choose us.",
                "Boost your productivity by 10x. No credit card required to get started.",
                "The smart choice for professionals. Award-winning platform trusted worldwide."
        };
        return descriptions[(c + g) % descriptions.length];
    }
}
