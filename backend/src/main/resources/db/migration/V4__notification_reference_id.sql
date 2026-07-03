-- Lets a later event (e.g. ride completed) find and remove an earlier, now-superseded
-- notification (e.g. ride started) tied to the same ride, instead of leaving stale
-- notifications sitting in the list forever.
ALTER TABLE notifications
    ADD COLUMN reference_id VARCHAR(64) NULL;

CREATE INDEX idx_notification_reference ON notifications (user_id, reference_id, event_type);
i