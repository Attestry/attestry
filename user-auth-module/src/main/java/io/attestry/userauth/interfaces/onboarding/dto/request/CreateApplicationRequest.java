package io.attestry.userauth.interfaces.onboarding.dto.request;

public record CreateApplicationRequest(
    String type,
    String orgName,
    String country,
    String bizRegNo,
    String evidenceBundleId
) {
}
