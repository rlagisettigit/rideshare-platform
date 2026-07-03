package com.rideshare.platform.common.exception;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String traceId() {
        String t = MDC.get("traceId");
        return t != null ? t : "unknown";
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        log.warn("[{}] {} - {}", traceId(), ex.getErrorCode(), ex.getMessage());
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(ex.getStatus().value())
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .category(ex.getCategory())
                .traceId(traceId())
                .build();
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("[{}] VALIDATION_001 - {}", traceId(), ex.getBindingResult().getFieldErrors());
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode("VALIDATION_001")
                .message("Request validation failed.")
                .category(ErrorCategory.VALIDATION)
                .traceId(traceId())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        log.warn("[{}] ENTITY_001 - {}", traceId(), ex.getMessage());
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .errorCode("ENTITY_001")
                .message(ex.getMessage())
                .category(ErrorCategory.BUSINESS)
                .traceId(traceId())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuth(BadCredentialsException ex) {
        log.warn("[{}] AUTH_001 - {}", traceId(), ex.getMessage());
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .errorCode("AUTH_001")
                .message("Invalid credentials.")
                .category(ErrorCategory.AUTHENTICATION)
                .traceId(traceId())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex) {
        log.warn("[{}] AUTHZ_001 - {}", traceId(), ex.getMessage());
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .errorCode("AUTHZ_001")
                .message("You do not have permission to perform this action.")
                .category(ErrorCategory.AUTHORIZATION)
                .traceId(traceId())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("[{}] SYS_001 - unhandled exception", traceId(), ex);
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode("SYS_001")
                .message("An unexpected error occurred.")
                .category(ErrorCategory.SYSTEM)
                .traceId(traceId())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
