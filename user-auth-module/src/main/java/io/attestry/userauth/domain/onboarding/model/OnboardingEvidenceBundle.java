package io.attestry.userauth.domain.onboarding.model;

import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class OnboardingEvidenceBundle {

    private static final int MAX_FILES = 5;

    private final String evidenceBundleId;
    private final String ownerUserId;
    private OnboardingEvidenceBundleStatus status;
    private final Instant createdAt;
    private Instant completedAt;
    private final List<OnboardingEvidenceFile> files;

    private OnboardingEvidenceBundle(
        String evidenceBundleId,
        String ownerUserId,
        OnboardingEvidenceBundleStatus status,
        Instant createdAt,
        Instant completedAt,
        List<OnboardingEvidenceFile> files
    ) {
        this.evidenceBundleId = evidenceBundleId;
        this.ownerUserId = ownerUserId;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.files = new ArrayList<>(files);
    }

    public static OnboardingEvidenceBundle create(String ownerUserId, Instant now) {
        validateRequired(ownerUserId, "ownerUserId");
        return new OnboardingEvidenceBundle(
            UUID.randomUUID().toString(),
            ownerUserId,
            OnboardingEvidenceBundleStatus.COLLECTING,
            now,
            null,
            List.of()
        );
    }

    public static OnboardingEvidenceBundle reconstitute(
        String evidenceBundleId,
        String ownerUserId,
        OnboardingEvidenceBundleStatus status,
        Instant createdAt,
        Instant completedAt,
        List<OnboardingEvidenceFile> files
    ) {
        return new OnboardingEvidenceBundle(evidenceBundleId, ownerUserId, status, createdAt, completedAt, files);
    }

    public OnboardingEvidenceFile addFile(String originalFileName, String contentType,
                                           String objectKey, Instant now) {
        assertCollecting();
        if (files.size() >= MAX_FILES) {
            throw new UserAuthDomainException(UserAuthErrorCode.EVIDENCE_FILE_LIMIT_EXCEEDED,
                "Maximum " + MAX_FILES + " evidence files are allowed per bundle");
        }
        OnboardingEvidenceFile file = OnboardingEvidenceFile.start(evidenceBundleId, originalFileName, contentType, objectKey, now);
        files.add(file);
        return file;
    }

    public void completeFile(String evidenceFileId, long sizeBytes, Instant now) {
        OnboardingEvidenceFile file = findFileOrThrow(evidenceFileId);
        completeFile(file, sizeBytes, now);
    }

    public void completeFile(OnboardingEvidenceFile file, long sizeBytes, Instant now) {
        int index = files.indexOf(file);
        OnboardingEvidenceFile completed = file.complete(sizeBytes, now);
        files.set(index, completed);
        if (files.stream().allMatch(OnboardingEvidenceFile::isReady)) {
            markReady(now);
        }
    }

    public OnboardingEvidenceFile findFileOrThrow(String evidenceFileId) {
        return findFile(evidenceFileId);
    }

    public void markReady(Instant now) {
        if (status == OnboardingEvidenceBundleStatus.READY) {
            return;
        }
        this.status = OnboardingEvidenceBundleStatus.READY;
        this.completedAt = now;
    }

    public void assertOwnedBy(String userId) {
        if (!ownerUserId.equals(userId)) {
            throw new UserAuthDomainException(UserAuthErrorCode.FORBIDDEN_SCOPE, "Evidence ownership mismatch");
        }
    }

    public void assertReady() {
        if (status != OnboardingEvidenceBundleStatus.READY) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_APPLICATION_STATE, "Evidence bundle is not ready");
        }
    }

    public void assertCollecting() {
        if (status != OnboardingEvidenceBundleStatus.COLLECTING) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_APPLICATION_STATE, "Evidence bundle is already completed");
        }
    }

    private OnboardingEvidenceFile findFile(String evidenceFileId) {
        return files.stream()
            .filter(f -> f.evidenceFileId().equals(evidenceFileId))
            .findFirst()
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.EVIDENCE_NOT_FOUND, "Evidence file not found in bundle"));
    }

    private static void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_APPLICATION_STATE, fieldName + " is required");
        }
    }

    // Getters
    public String evidenceBundleId() { return evidenceBundleId; }
    public String ownerUserId() { return ownerUserId; }
    public OnboardingEvidenceBundleStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant completedAt() { return completedAt; }
    public List<OnboardingEvidenceFile> files() { return Collections.unmodifiableList(files); }
}
