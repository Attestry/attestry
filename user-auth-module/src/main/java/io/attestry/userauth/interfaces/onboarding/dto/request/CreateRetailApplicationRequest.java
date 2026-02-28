package io.attestry.userauth.interfaces.onboarding.dto.request;

public record CreateRetailApplicationRequest(
    String retailName,
    String country,
    String bizRegNo,
    String evidenceBundleId
) {
}
