package io.attestry.userauth.infrastructure.persistence.jpa.repository;

import io.attestry.userauth.infrastructure.persistence.jpa.entity.OnboardingEvidenceFileJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingEvidenceFileJpaRepository extends JpaRepository<OnboardingEvidenceFileJpaEntity, String> {
    List<OnboardingEvidenceFileJpaEntity> findByEvidenceBundleId(String evidenceBundleId);
}
