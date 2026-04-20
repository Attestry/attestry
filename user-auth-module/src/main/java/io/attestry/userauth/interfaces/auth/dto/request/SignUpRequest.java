package io.attestry.userauth.interfaces.auth.dto.request;

import io.attestry.userauth.domain.auth.model.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignUpRequest(
    @NotBlank(message = "Email is required")
    @Pattern(regexp = Email.VALIDATION_PATTERN, message = "Please enter a valid email format.")
    String email,

    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[A-Z]).{8,}$",
        message = "Password must be at least 8 characters and contain at least one uppercase letter"
    )
    String password,

    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^010-(?!0000)\\d{4}-\\d{4}$",
        message = "Phone number must be in 010-XXXX-XXXX format, middle digits cannot be 0000"
    )
    String phone
) {
}
