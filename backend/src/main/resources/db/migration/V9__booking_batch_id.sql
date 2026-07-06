-- Correlates bookings created together via "book all upcoming occurrences" on a recurring
-- ride, so the driver can accept/reject the whole batch in one action instead of once per date.
ALTER TABLE bookings ADD COLUMN booking_batch_id VARCHAR(36) NULL;
ALTER TABLE bookings ADD INDEX idx_booking_batch (booking_batch_id);
