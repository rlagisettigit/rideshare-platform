package com.rideshare.platform.search.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.search.dto.RideSearchRequest;
import com.rideshare.platform.search.dto.RideSearchResult;
import com.rideshare.platform.search.service.SearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** FR: Section 8 Ride Search. Target: P95 < 300ms (Section 24). */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search")
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/rides")
    public ApiResponse<List<RideSearchResult>> search(@Valid @RequestBody RideSearchRequest request) {
        return ApiResponse.ok(searchService.search(request));
    }
}
