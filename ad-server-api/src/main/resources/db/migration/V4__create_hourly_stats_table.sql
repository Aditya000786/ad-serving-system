CREATE TABLE hourly_stats (
    campaign_id VARCHAR(36) NOT NULL,
    ad_id VARCHAR(36) NOT NULL,
    hour TIMESTAMP WITH TIME ZONE NOT NULL,
    impressions BIGINT NOT NULL DEFAULT 0,
    clicks BIGINT NOT NULL DEFAULT 0,
    conversions BIGINT NOT NULL DEFAULT 0,
    spend BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (campaign_id, ad_id, hour)
);

CREATE INDEX idx_hourly_campaign_hour ON hourly_stats (campaign_id, hour);
