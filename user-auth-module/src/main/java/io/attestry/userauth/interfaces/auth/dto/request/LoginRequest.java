package io.attestry.userauth.interfaces.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
    @NotBlank(message = "email is required")
    @Email(message = "email format is invalid")
    String email,

    @NotBlank(message = "password is required")
    @Pattern(
        regexp = "^(?=.*[A-Z]).{8,}$",
        message = "password must be at least 8 characters and include an uppercase letter"
    )
    String password,

    String tenantId
) {
}
