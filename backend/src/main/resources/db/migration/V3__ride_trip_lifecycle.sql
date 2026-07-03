-- Adds trip start/end timestamps, needed to take a ride from CONFIRMED bookings through
-- IN_PROGRESS to FINISHED (see RideService.start()/finish()).

ALTER TABLE rides
    ADD COLUMN actual_start_time TIMESTAMP NULL,
    ADD COLUMN actual_end_time TIMESTAMP NULL;