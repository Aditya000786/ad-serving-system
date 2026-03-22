CREATE TABLE ads (
    id VARCHAR(36) PRIMARY KEY,
    ad_group_id VARCHAR(36) NOT NULL REFERENCES ad_groups(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    creative_url VARCHAR(1024) NOT NULL,
    click_url VARCHAR(1024) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ads_ad_group ON ads (ad_group_id);
CREATE INDEX idx_ads_status ON ads (status);
