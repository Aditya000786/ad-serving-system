CREATE TABLE daily_stats (
    campaign_id VARCHAR(36) NOT NULL,
    ad_id VARCHAR(36) NOT NULL,
    date DATE NOT NULL,
    impressions BIGINT NOT NULL DEFAULT 0,
    clicks BIGINT NOT NULL DEFAULT 0,
    conversions BIGINT NOT NULL DEFAULT 0,
    spend BIGINT NOT NULL DEFAULT 0,
    avg_cpc DOUBLE PRECISION DEFAULT 0,
    ctr DOUBLE PRECISION DEFAULT 0,
    PRIMARY KEY (campaign_id, ad_id, date)
);

CREATE INDEX idx_daily_campaign_date ON daily_stats (campaign_id, date);
