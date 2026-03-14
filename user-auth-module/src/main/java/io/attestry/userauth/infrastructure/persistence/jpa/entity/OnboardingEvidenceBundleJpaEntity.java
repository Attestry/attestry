package io.attestry.userauth.infrastructure.persistence.jpa.entity;

import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceBundleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "onboarding_evidence_bundles")
public class OnboardingEvidenceBundleJpaEntity {

    @Id
    @Column(name = "evidence_bundle_id", nullable = false, length = 36)
    private String evidenceBundleId;

    @Column(name = "owner_user_id", nullable = false, length = 36)
    private String ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OnboardingEvidenceBundleStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected OnboardingEvidenceBundleJpaEntity() {
    }

    public OnboardingEvidenceBundleJpaEntity(
        String evidenceBundleId,
        String ownerUserId,
        OnboardingEvidenceBundleStatus status,
        Instant createdAt,
        Instant completedAt
    ) {
        this.evidenceBundleId = evidenceBundleId;
        this.ownerUserId = ownerUserId;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public String getEvidenceBundleId() {
        return evidenceBundleId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public OnboardingEvidenceBundleStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
