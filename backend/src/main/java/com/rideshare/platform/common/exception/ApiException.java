package com.rideshare.platform.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final ErrorCategory category;

    public ApiException(HttpStatus status, String errorCode, ErrorCategory category, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.category = category;
    }

    public static ApiException notFound(String errorCode, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, errorCode, ErrorCategory.BUSINESS, message);
    }

    public static ApiException badRequest(String errorCode, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, errorCode, ErrorCategory.VALIDATION, message);
    }

    public static ApiException conflict(String errorCode, String message) {
        return new ApiException(HttpStatus.CONFLICT, errorCode, ErrorCategory.BUSINESS, message);
    }

    public static ApiException businessRule(String errorCode, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, errorCode, ErrorCategory.BUSINESS, message);
    }

    public static ApiException unauthorized(String errorCode, String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, errorCode, ErrorCategory.AUTHENTICATION, message);
    }

    public static ApiException forbidden(String errorCode, String message) {
        return new ApiException(HttpStatus.FORBIDDEN, errorCode, ErrorCategory.AUTHORIZATION, message);
    }

    public static ApiException externalService(String errorCode, String message) {
        return new ApiException(HttpStatus.BAD_GATEWAY, errorCode, ErrorCategory.EXTERNAL_SERVICE, message);
    }
}
