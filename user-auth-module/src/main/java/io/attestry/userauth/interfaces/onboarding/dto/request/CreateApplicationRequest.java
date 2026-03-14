package io.attestry.userauth.interfaces.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;

//TODO("country는 enum으로 매핑 ")
public record CreateApplicationRequest(
        @NotBlank(message = "type is required")
        String type,
        @NotBlank(message = "orgName is required")
        String orgName,
        @NotBlank(message = "country is required")
        String country,
        String address,
        @NotBlank(message = "bizRegNo is required")
        String bizRegNo,
        @NotBlank(message = "evidenceBundleId is required")
        String evidenceBundleId
) {
}
