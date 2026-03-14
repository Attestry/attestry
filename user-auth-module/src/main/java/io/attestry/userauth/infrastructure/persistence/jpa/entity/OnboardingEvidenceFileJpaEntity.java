package io.attestry.userauth.infrastructure.persistence.jpa.entity;

import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceFileStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "onboarding_evidence_files")
public class OnboardingEvidenceFileJpaEntity {

    @Id
    @Column(name = "evidence_file_id", nullable = false, length = 36)
    private String evidenceFileId;

    @Column(name = "evidence_bundle_id", nullable = false, length = 36)
    private String evidenceBundleId;

    @Column(name = "object_key", nullable = false, unique = true, length = 512)
    private String objectKey;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 255)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OnboardingEvidenceFileStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected OnboardingEvidenceFileJpaEntity() {
    }

    public OnboardingEvidenceFileJpaEntity(
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
        this.evidenceFileId = evidenceFileId;
        this.evidenceBundleId = evidenceBundleId;
        this.objectKey = objectKey;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public String getEvidenceFileId() {
        return evidenceFileId;
    }

    public String getEvidenceBundleId() {
        return evidenceBundleId;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public OnboardingEvidenceFileStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
