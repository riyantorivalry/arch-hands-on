-- Phase 3: Analytics event capture
-- Stores user behavior and system events for analytics and reporting

CREATE TABLE analytics_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64),
    workspace_id VARCHAR(64),
    event_type VARCHAR(100) NOT NULL,
    event_category VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50),
    resource_id VARCHAR(64),
    event_data JSON NOT NULL,
    session_id VARCHAR(64),
    user_agent VARCHAR(500),
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(64),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(64)
);

-- Indexes for efficient querying
CREATE INDEX idx_analytics_events_tenant ON analytics_events (tenant_id);
CREATE INDEX idx_analytics_events_user ON analytics_events (user_id);
CREATE INDEX idx_analytics_events_type ON analytics_events (event_type);
CREATE INDEX idx_analytics_events_timestamp ON analytics_events (created_at);
CREATE INDEX idx_analytics_events_category ON analytics_events (event_category);
CREATE INDEX idx_analytics_events_resource ON analytics_events (resource_type, resource_id);
