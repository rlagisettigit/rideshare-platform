package com.rideshare.platform.reporting.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.reporting.service.ReportingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** FR: Section 17 Reporting. Access restricted to ROLE_ADMIN / ROLE_FINANCE. */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports")
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/daily-rides")
    public ApiResponse<List<Map<String, Object>>> dailyRides(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(reportingService.dailyRides(days));
    }

    @GetMapping("/completion-cancellation")
    public ApiResponse<Map<String, Object>> completionCancellation() {
        return ApiResponse.ok(reportingService.completionAndCancellationRates());
    }

    @GetMapping("/peak-hours")
    public ApiResponse<List<Map<String, Object>>> peakHours() {
        return ApiResponse.ok(reportingService.peakHours());
    }

    @GetMapping("/driver-earnings")
    public ApiResponse<List<Map<String, Object>>> driverEarnings() {
        return ApiResponse.ok(reportingService.driverEarnings());
    }
}
