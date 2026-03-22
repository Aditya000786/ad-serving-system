CREATE TABLE ad_groups (
    id VARCHAR(36) PRIMARY KEY,
    campaign_id VARCHAR(36) NOT NULL REFERENCES campaigns(id),
    name VARCHAR(255) NOT NULL,
    targeting_criteria JSONB NOT NULL DEFAULT '{}',
    bid_amount_cents BIGINT NOT NULL,
    bid_type VARCHAR(10) NOT NULL DEFAULT 'CPC',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ad_groups_campaign ON ad_groups (campaign_id);
