package io.attestry.userauth.interfaces.onboarding.dto.response;

import io.attestry.userauth.application.dto.result.EvidenceBundleResult;

public record EvidenceBundleResponse(
    String evidenceBundleId,
    String status
) {
    public static EvidenceBundleResponse from(EvidenceBundleResult result) {
        return new EvidenceBundleResponse(result.evidenceBundleId(), result.status());
    }
}
