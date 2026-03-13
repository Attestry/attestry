package io.attestry.userauth.domain.onboarding.model;

import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;import java.time.Instant;
import java.util.UUID;

public record OnboardingEvidenceFile(
    String evidenceFileId,
    String evidenceBundleId,
    String objectKey,
    String originalFileName,
    String contentType,
    Long sizeBytes,
    OnboardingEvidenceFileStatus status,
    Instant createdAt,
    Instant completedAt
) {
    public static OnboardingEvidenceFile start(
        String evidenceBundleId,
        String originalFileName,
        String contentType,
        String objectKey,
        Instant now
    ) {
        validateRequired(evidenceBundleId, "evidenceBundleId");
        validateRequired(originalFileName, "originalFileName");
        validateRequired(contentType, "contentType");
        validateRequired(objectKey, "objectKey");
        validatePdfOnly(originalFileName, contentType);
        return new OnboardingEvidenceFile(
            UUID.randomUUID().toString(),
            evidenceBundleId,
            objectKey,
            originalFileName,
            contentType,
            null,
            OnboardingEvidenceFileStatus.PENDING_UPLOAD,
            now,
            null
        );
    }

    public OnboardingEvidenceFile complete(long sizeBytes, Instant now) {
        if (status != OnboardingEvidenceFileStatus.PENDING_UPLOAD) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_APPLICATION_STATE, "Evidence file is not pending upload");
        }
        if (sizeBytes <= 0) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_APPLICATION_STATE, "Evidence size must be positive");
        }
        return new OnboardingEvidenceFile(
            evidenceFileId,
            evidenceBundleId,
            objectKey,
            originalFileName,
            contentType,
            sizeBytes,
            OnboardingEvidenceFileStatus.READY,
            createdAt,
            now
        );
    }

    public void assertBelongsToBundle(String bundleId) {
        if (!evidenceBundleId.equals(bundleId)) {
            throw new UserAuthDomainException(UserAuthErrorCode.EVIDENCE_NOT_FOUND, "Evidence file does not belong to bundle");
        }
    }

    public boolean isReady() {
        return status == OnboardingEvidenceFileStatus.READY;
    }

    private static void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_APPLICATION_STATE, fieldName + " is required");
        }
    }

    private static void validatePdfOnly(String originalFileName, String contentType) {
        if (!"application/pdf".equalsIgnoreCase(contentType)) {
            throw new UserAuthDomainException(UserAuthErrorCode.EVIDENCE_FILE_TYPE_NOT_ALLOWED, "Only PDF content type is allowed");
        }
        if (!originalFileName.toLowerCase().endsWith(".pdf")) {
            throw new UserAuthDomainException(UserAuthErrorCode.EVIDENCE_FILE_TYPE_NOT_ALLOWED, "Only .pdf files are allowed");
        }
    }
}
