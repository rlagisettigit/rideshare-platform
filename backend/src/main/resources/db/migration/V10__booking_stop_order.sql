-- Pickup/drop-off visiting order computed when the driver starts a multi-passenger ride
-- (see RideStopPlanner) - distinct from pickup_sequence_no/drop_sequence_no, which locate a
-- booking along the ride's fixed route polyline, not its visiting order relative to other
-- passengers on the same trip. NULL until the ride is started.
ALTER TABLE bookings
    ADD COLUMN pickup_stop_order INT NULL,
    ADD COLUMN drop_stop_order   INT NULL;
