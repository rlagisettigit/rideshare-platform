package com.rideshare.platform.reporting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * FR: Section 17 Reporting - Daily Rides, Revenue, Driver Earnings, Passenger Growth,
 * Ride Completion %, Cancellation %, Average Rating, Heat Maps, Peak Hours.
 * Implemented as read-model queries against the operational tables; at scale these should
 * run against a reporting replica or a materialized aggregate table refreshed via Kafka
 * stream processors consuming ride.* / booking.* events.
 */
@Service
@RequiredArgsConstructor
public class ReportingService {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> dailyRides(int days) {
        return jdbcTemplate.queryForList("""
                SELECT DATE(departure_at) as day, COUNT(*) as ride_count
                FROM rides
                WHERE departure_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
                GROUP BY DATE(departure_at)
                ORDER BY day DESC
                """, days);
    }

    public Map<String, Object> completionAndCancellationRates() {
        return jdbcTemplate.queryForMap("""
                SELECT
                  ROUND(100.0 * SUM(CASE WHEN status = 'FINISHED' THEN 1 ELSE 0 END) / COUNT(*), 2) AS completion_pct,
                  ROUND(100.0 * SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) / COUNT(*), 2) AS cancellation_pct
                FROM rides
                """);
    }

    public List<Map<String, Object>> peakHours() {
        return jdbcTemplate.queryForList("""
                SELECT HOUR(departure_at) as hour_of_day, COUNT(*) as ride_count
                FROM rides
                GROUP BY HOUR(departure_at)
                ORDER BY ride_count DESC
                """);
    }

    public List<Map<String, Object>> driverEarnings() {
        return jdbcTemplate.queryForList("""
                SELECT d.id as driver_id, u.name as driver_name, COALESCE(SUM(b.fare), 0) as total_earnings
                FROM drivers d
                JOIN users u ON u.id = d.user_id
                JOIN rides r ON r.driver_id = d.id
                JOIN bookings b ON b.ride_id = r.id AND b.status IN ('CONFIRMED', 'COMPLETED')
                GROUP BY d.id, u.name
                ORDER BY total_earnings DESC
                """);
    }
}
