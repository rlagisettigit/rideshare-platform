package com.rideshare.platform.admin.controller;

import com.rideshare.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * FR: Section 16 Admin Portal - Support Tickets module.
 * Scaffolded CRUD surface; wire to a SupportTicketService/Repository (see support_tickets table
 * in V1__init.sql) following the same pattern as AdminDriverController.
 */
@RestController
@RequestMapping("/api/v1/support/tickets")
@Tag(name = "Support Tickets")
public class AdminSupportTicketController {

    @GetMapping
    public ApiResponse<String> notImplemented() {
        return ApiResponse.ok("Support ticket listing - implement against support_tickets table.");
    }
}
