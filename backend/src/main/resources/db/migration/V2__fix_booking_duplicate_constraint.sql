-- V1's uq_no_duplicate_booking (ride_id, passenger_id, status) incorrectly blocks a passenger
-- from ever having more than one CANCELLED/REJECTED booking on the same ride (e.g. book ->
-- cancel -> book again -> cancel again hits the same unique key). The actual business rule -
-- already enforced in BookingService.createBooking() via
-- existsByRideIdAndPassengerIdAndStatusIn(..., [PENDING, CONFIRMED]) - is "at most one
-- simultaneously ACTIVE booking per passenger per ride", not "at most one booking per status
-- ever". Replace the constraint with a generated column that's non-NULL only while a booking
-- is PENDING/CONFIRMED; MySQL allows unlimited NULLs in a unique index, so CANCELLED/REJECTED/
-- COMPLETED rows never collide.

ALTER TABLE bookings
    DROP INDEX uq_no_duplicate_booking;

ALTER TABLE bookings
    ADD COLUMN active_booking_key BIGINT
    GENERATED ALWAYS AS (CASE WHEN status IN ('PENDING', 'CONFIRMED') THEN passenger_id END) STORED;

ALTER TABLE bookings
    ADD UNIQUE KEY uq_no_duplicate_active_booking (ride_id, active_booking_key);
