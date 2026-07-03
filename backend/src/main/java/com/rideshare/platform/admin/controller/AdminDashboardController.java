package com.rideshare.platform.admin.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.driver.entity.DriverStatus;
import com.rideshare.platform.driver.repository.DriverRepository;
import com.rideshare.platform.ride.entity.RideStatus;
import com.rideshare.platform.ride.repository.RideRepository;
import com.rideshare.platform.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** FR: Section 16 Admin Portal - Dashboard module. Access restricted to ROLE_ADMIN (see SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin - Dashboard")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.ok(Map.of(
                "totalUsers", userRepository.count(),
                "pendingDriverVerifications", driverRepository.countByStatus(DriverStatus.PENDING),
                "activeRides", rideRepository.findByStatus(RideStatus.ACTIVE).size()
        ));
    }
}
