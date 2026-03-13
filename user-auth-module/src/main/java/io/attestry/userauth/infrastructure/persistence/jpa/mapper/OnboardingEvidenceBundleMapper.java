package io.attestry.userauth.infrastructure.persistence.jpa.mapper;

import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceBundle;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceFile;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.OnboardingEvidenceBundleJpaEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingEvidenceBundleMapper {

    private final OnboardingEvidenceFileMapper fileMapper;

    public OnboardingEvidenceBundle toDomain(OnboardingEvidenceBundleJpaEntity entity, List<OnboardingEvidenceFile> files) {
        if (entity == null) {
            return null;
        }
        return OnboardingEvidenceBundle.reconstitute(
            entity.getEvidenceBundleId(),
            entity.getOwnerUserId(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getCompletedAt(),
            files
        );
    }

    public OnboardingEvidenceBundleJpaEntity toEntity(OnboardingEvidenceBundle domain) {
        if (domain == null) {
            return null;
        }
        return new OnboardingEvidenceBundleJpaEntity(
            domain.evidenceBundleId(),
            domain.ownerUserId(),
            domain.status(),
            domain.createdAt(),
            domain.completedAt()
        );
    }

    public OnboardingEvidenceFileMapper fileMapper() {
        return fileMapper;
    }
}
