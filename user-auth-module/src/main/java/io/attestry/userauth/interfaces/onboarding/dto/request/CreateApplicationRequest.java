package io.attestry.userauth.interfaces.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

//TODO("Map country to enum")
public record CreateApplicationRequest(
        @NotBlank(message = "Application type is required")
        String type,
        @NotBlank(message = "Organization name is required")
        String orgName,
        @NotBlank(message = "Country is required")
        String country,
        @NotBlank(message = "Address is required")
        String address,
        @NotBlank(message = "Business registration number is required")
        @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{5}$", message = "Business registration number must be in 123-45-67890 format.")
        String bizRegNo,
        @NotBlank(message = "Evidence bundle ID is required")
        String evidenceBundleId
) {
}
