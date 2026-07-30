ALTER TABLE outbox_events
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0;
