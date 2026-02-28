package io.attestry.userauth.domain.onboarding.model;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import java.time.Instant;
import java.util.UUID;

public record OnboardingEvidenceBundle(
    String evidenceBundleId,
    String ownerUserId,
    OnboardingEvidenceBundleStatus status,
    Instant createdAt,
    Instant completedAt
) {
    public static OnboardingEvidenceBundle create(String ownerUserId, Instant now) {
        validateRequired(ownerUserId, "ownerUserId");
        return new OnboardingEvidenceBundle(
            UUID.randomUUID().toString(),
            ownerUserId,
            OnboardingEvidenceBundleStatus.COLLECTING,
            now,
            null
        );
    }

    public OnboardingEvidenceBundle markReady(Instant now) {
        if (status == OnboardingEvidenceBundleStatus.READY) {
            return this;
        }
        return new OnboardingEvidenceBundle(
            evidenceBundleId,
            ownerUserId,
            OnboardingEvidenceBundleStatus.READY,
            createdAt,
            now
        );
    }

    public void assertOwnedBy(String userId) {
        if (!ownerUserId.equals(userId)) {
            throw new DomainException(ErrorCode.FORBIDDEN_SCOPE, "Evidence ownership mismatch");
        }
    }

    public void assertReady() {
        if (status != OnboardingEvidenceBundleStatus.READY) {
            throw new DomainException(ErrorCode.INVALID_APPLICATION_STATE, "Evidence bundle is not ready");
        }
    }

    public void assertCollecting() {
        if (status != OnboardingEvidenceBundleStatus.COLLECTING) {
            throw new DomainException(ErrorCode.INVALID_APPLICATION_STATE, "Evidence bundle is already completed");
        }
    }

    private static void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new DomainException(ErrorCode.INVALID_APPLICATION_STATE, fieldName + " is required");
        }
    }
}
