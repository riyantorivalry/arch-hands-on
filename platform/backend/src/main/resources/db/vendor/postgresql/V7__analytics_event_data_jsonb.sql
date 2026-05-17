-- PostgreSQL analytics comparison storage.
-- Keep the base migration portable for H2 tests, then convert the production
-- PostgreSQL column to JSONB and add a GIN index for JSON property queries.

ALTER TABLE analytics_events
    ALTER COLUMN event_data TYPE jsonb
    USING event_data::jsonb;

CREATE INDEX idx_analytics_events_event_data_gin
    ON analytics_events USING gin (event_data);
