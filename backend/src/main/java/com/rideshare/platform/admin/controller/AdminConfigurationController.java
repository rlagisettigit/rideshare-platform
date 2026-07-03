package com.rideshare.platform.admin.controller;

import com.rideshare.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** FR: Section 16 Admin Portal - Configuration module (system_configuration table). */
@RestController
@RequestMapping("/api/v1/admin/configuration")
@RequiredArgsConstructor
@Tag(name = "Admin - Configuration")
public class AdminConfigurationController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(jdbcTemplate.queryForList("SELECT * FROM system_configuration"));
    }

    public record UpdateConfigRequest(String value) {}

    @PutMapping("/{key}")
    public ApiResponse<Void> update(@PathVariable("key") String key, @RequestBody UpdateConfigRequest request) {
        jdbcTemplate.update("UPDATE system_configuration SET config_value = ? WHERE config_key = ?",
                request.value(), key);
        // Section 18 Audit: CONFIGURATION_CHANGE should be recorded here via AuditService.
        return ApiResponse.ok(null, "Configuration updated.");
    }
}
