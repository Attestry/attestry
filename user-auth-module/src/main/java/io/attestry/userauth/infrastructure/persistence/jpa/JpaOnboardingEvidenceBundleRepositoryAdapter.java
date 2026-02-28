package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.application.port.OnboardingEvidenceBundleRepositoryPort;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceBundle;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.OnboardingEvidenceBundleJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.OnboardingEvidenceBundleJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaOnboardingEvidenceBundleRepositoryAdapter implements OnboardingEvidenceBundleRepositoryPort {

    private final OnboardingEvidenceBundleJpaRepository repository;

    public JpaOnboardingEvidenceBundleRepositoryAdapter(OnboardingEvidenceBundleJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public OnboardingEvidenceBundle save(OnboardingEvidenceBundle bundle) {
        OnboardingEvidenceBundleJpaEntity saved = repository.save(new OnboardingEvidenceBundleJpaEntity(
            bundle.evidenceBundleId(),
            bundle.ownerUserId(),
            bundle.status(),
            bundle.createdAt(),
            bundle.completedAt()
        ));
        return toDomain(saved);
    }

    @Override
    public Optional<OnboardingEvidenceBundle> findById(String evidenceBundleId) {
        return repository.findById(evidenceBundleId).map(this::toDomain);
    }

    private OnboardingEvidenceBundle toDomain(OnboardingEvidenceBundleJpaEntity entity) {
        return new OnboardingEvidenceBundle(
            entity.getEvidenceBundleId(),
            entity.getOwnerUserId(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getCompletedAt()
        );
    }
}
