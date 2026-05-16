-- Phase 2: Outbox pattern for reliable event publishing
-- Stores events in database before publishing to ensure no event loss

CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    event_data JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP WITH TIME ZONE NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT NULL
);

-- Index for efficient querying of unpublished events
-- Note: H2 doesn't support partial indexes with WHERE, so we index all events
CREATE INDEX idx_outbox_events_unpublished ON outbox_events (published, created_at);

-- Index for tenant isolation
CREATE INDEX idx_outbox_events_tenant ON outbox_events (tenant_id, published, created_at);

-- Index for event type filtering
CREATE INDEX idx_outbox_events_type ON outbox_events (event_type, published, created_at);

-- Index for cleanup of old published events
CREATE INDEX idx_outbox_events_published_at ON outbox_events (published_at);
