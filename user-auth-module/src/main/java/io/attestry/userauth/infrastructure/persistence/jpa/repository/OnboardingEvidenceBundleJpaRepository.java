package io.attestry.userauth.infrastructure.persistence.jpa.repository;

import io.attestry.userauth.infrastructure.persistence.jpa.entity.OnboardingEvidenceBundleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingEvidenceBundleJpaRepository extends JpaRepository<OnboardingEvidenceBundleJpaEntity, String> {
}
