package com.rideshare.platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** FR-001 User Registration. */
public record RegisterRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid mobile number") String mobile,
        // FR-001 Password policy: min 8 chars, at least one letter and one digit
        @NotBlank @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "Password must be at least 8 characters and include a letter and a digit"
        ) String password,
        boolean asDriver
) {}
