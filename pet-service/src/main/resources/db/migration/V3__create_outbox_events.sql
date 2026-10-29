-- Transactional outbox: side effects (Kafka events, billing gRPC calls) are written here
-- in the same transaction as the business data, then delivered by OutboxRelay after commit.
CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    payload BYTEA NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    next_attempt_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP
);

-- The relay only ever scans undelivered rows
CREATE INDEX idx_outbox_pending ON outbox_events (next_attempt_at) WHERE processed_at IS NULL;
