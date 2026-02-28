package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.application.port.OnboardingEvidenceFileRepositoryPort;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceFile;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.OnboardingEvidenceFileJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.OnboardingEvidenceFileJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaOnboardingEvidenceFileRepositoryAdapter implements OnboardingEvidenceFileRepositoryPort {

    private final OnboardingEvidenceFileJpaRepository repository;

    public JpaOnboardingEvidenceFileRepositoryAdapter(OnboardingEvidenceFileJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public OnboardingEvidenceFile save(OnboardingEvidenceFile file) {
        OnboardingEvidenceFileJpaEntity saved = repository.save(new OnboardingEvidenceFileJpaEntity(
            file.evidenceFileId(),
            file.evidenceBundleId(),
            file.objectKey(),
            file.originalFileName(),
            file.contentType(),
            file.sizeBytes(),
            file.status(),
            file.createdAt(),
            file.completedAt()
        ));
        return toDomain(saved);
    }

    @Override
    public Optional<OnboardingEvidenceFile> findById(String evidenceFileId) {
        return repository.findById(evidenceFileId).map(this::toDomain);
    }

    @Override
    public List<OnboardingEvidenceFile> findByBundleId(String evidenceBundleId) {
        return repository.findByEvidenceBundleId(evidenceBundleId).stream()
            .map(this::toDomain)
            .toList();
    }

    private OnboardingEvidenceFile toDomain(OnboardingEvidenceFileJpaEntity entity) {
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
}
