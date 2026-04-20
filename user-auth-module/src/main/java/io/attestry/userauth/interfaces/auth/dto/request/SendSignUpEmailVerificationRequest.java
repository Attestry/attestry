package io.attestry.userauth.interfaces.auth.dto.request;

import io.attestry.userauth.domain.auth.model.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendSignUpEmailVerificationRequest(
    @NotBlank(message = "Email is required")
    @Pattern(regexp = Email.VALIDATION_PATTERN, message = "Please enter a valid email format.")
    String email
) {
}
