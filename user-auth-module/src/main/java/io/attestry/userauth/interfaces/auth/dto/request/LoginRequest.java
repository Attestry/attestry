package io.attestry.userauth.interfaces.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[A-Z]).{8,}$",
        message = "Password must be at least 8 characters and contain at least one uppercase letter"
    )
    String password,

    String tenantId
) {
}
