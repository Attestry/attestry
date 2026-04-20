package io.attestry.userauth.interfaces.auth.dto.request;

import io.attestry.userauth.domain.auth.model.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmSignUpEmailVerificationRequest(
    @NotBlank(message = "Email is required")
    @Pattern(regexp = Email.VALIDATION_PATTERN, message = "Please enter a valid email format.")
    String email,

    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "^\\d{8}$", message = "Verification code must be 8 digits")
    String code
) {
}
