package io.attestry.userauth.interfaces.onboarding.dto.request;

public record CreateBrandApplicationRequest(
    String brandName,
    String country,
    String bizRegNo,
    String evidenceBundleId
) {
}
