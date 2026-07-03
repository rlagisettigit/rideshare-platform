package com.rideshare.platform.route.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * FR: Section 7 Route Management - "Ride -> Ordered H3 Sequence".
 * One row per waypoint along the decoded polyline, in travel order.
 */
@Getter
@Setter
@Entity
@Table(name = "ride_route_points")
public class RideRoutePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ride_id", nullable = false)
    private Long rideId;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    private double lat;
    private double lng;

    @Column(name = "h3_cell", nullable = false)
    private String h3Cell;

    @Column(name = "cumulative_distance_m", nullable = false)
    private Integer cumulativeDistanceM = 0;
}
