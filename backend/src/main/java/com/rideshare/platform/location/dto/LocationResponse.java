package com.rideshare.platform.location.dto;

import java.time.LocalDateTime;

public record LocationResponse(double lat, double lng, Double heading, LocalDateTime updatedAt) {}
