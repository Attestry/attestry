package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.application.port.OnboardingEvidenceBundleRepositoryPort;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceBundle;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceFile;
import io.attestry.userauth.domain.onboarding.repository.EvidenceBundleRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.OnboardingEvidenceBundleJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.OnboardingEvidenceFileJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.OnboardingEvidenceBundleJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.OnboardingEvidenceFileJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaOnboardingEvidenceBundleRepositoryAdapter
    implements OnboardingEvidenceBundleRepositoryPort, EvidenceBundleRepository {

    private final OnboardingEvidenceBundleJpaRepository bundleRepository;
    private final OnboardingEvidenceFileJpaRepository fileRepository;

    public JpaOnboardingEvidenceBundleRepositoryAdapter(
        OnboardingEvidenceBundleJpaRepository bundleRepository,
        OnboardingEvidenceFileJpaRepository fileRepository
    ) {
        this.bundleRepository = bundleRepository;
        this.fileRepository = fileRepository;
    }

    @Override
    public OnboardingEvidenceBundle save(OnboardingEvidenceBundle bundle) {
        bundleRepository.save(new OnboardingEvidenceBundleJpaEntity(
            bundle.evidenceBundleId(),
            bundle.ownerUserId(),
            bundle.status(),
            bundle.createdAt(),
            bundle.completedAt()
        ));
        for (OnboardingEvidenceFile file : bundle.files()) {
            fileRepository.save(new OnboardingEvidenceFileJpaEntity(
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
        }
        return bundle;
    }

    @Override
    public Optional<OnboardingEvidenceBundle> findById(String evidenceBundleId) {
        return bundleRepository.findById(evidenceBundleId).map(entity -> {
            List<OnboardingEvidenceFile> files = fileRepository.findByEvidenceBundleId(evidenceBundleId).stream()
                .map(this::toFileDomain)
                .toList();
            return OnboardingEvidenceBundle.reconstitute(
                entity.getEvidenceBundleId(),
                entity.getOwnerUserId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                files
            );
        });
    }

    @Override
    public OnboardingEvidenceFile saveFile(OnboardingEvidenceFile file) {
        OnboardingEvidenceFileJpaEntity saved = fileRepository.save(new OnboardingEvidenceFileJpaEntity(
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
        return toFileDomain(saved);
    }

    @Override
    public Optional<OnboardingEvidenceFile> findFileById(String evidenceFileId) {
        return fileRepository.findById(evidenceFileId).map(this::toFileDomain);
    }

    @Override
    public List<OnboardingEvidenceFile> findFilesByBundleId(String evidenceBundleId) {
        return fileRepository.findByEvidenceBundleId(evidenceBundleId).stream()
            .map(this::toFileDomain)
            .toList();
    }

    private OnboardingEvidenceFile toFileDomain(OnboardingEvidenceFileJpaEntity entity) {
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
