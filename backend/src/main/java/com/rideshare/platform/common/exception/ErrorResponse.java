package com.rideshare.platform.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * FR: Section 21 - Error Handling. Matches the standard format specified in the SRS:
 * {
 *   "timestamp": "...",
 *   "status": 400,
 *   "errorCode": "RIDE_001",
 *   "message": "Available seats exceeded.",
 *   "traceId": "..."
 * }
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String errorCode;
    private String message;
    private String traceId;
    private ErrorCategory category;
    private List<FieldError> fieldErrors;

    @Getter
    @Builder
    public static class FieldError {
        private String field;
        private String message;
    }
}
