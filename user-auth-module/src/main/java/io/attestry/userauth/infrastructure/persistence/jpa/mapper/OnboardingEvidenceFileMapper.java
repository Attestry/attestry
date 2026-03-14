package io.attestry.userauth.infrastructure.persistence.jpa.mapper;

import io.attestry.commonlib.infrastructure.DomainMapper;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceFile;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.OnboardingEvidenceFileJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OnboardingEvidenceFileMapper implements DomainMapper<OnboardingEvidenceFile, OnboardingEvidenceFileJpaEntity> {

    @Override
    public OnboardingEvidenceFile toDomain(OnboardingEvidenceFileJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new OnboardingEvidenceFile(
            entity.getEvidenceFileId(),
            entity.getEvidenceBundleId(),
            entity.getObjectKey(),
            entity.getOriginalFileName(),
            entity.getContentType(),
            entity.getSizeBytes(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getCompletedAt()
        );
    }

    @Override
    public OnboardingEvidenceFileJpaEntity toEntity(OnboardingEvidenceFile domain) {
        if (domain == null) {
            return null;
        }
        return new OnboardingEvidenceFileJpaEntity(
            domain.evidenceFileId(),
            domain.evidenceBundleId(),
            domain.objectKey(),
            domain.originalFileName(),
            domain.contentType(),
            domain.sizeBytes(),
            domain.status(),
            domain.createdAt(),
            domain.completedAt()
        );
    }
}
