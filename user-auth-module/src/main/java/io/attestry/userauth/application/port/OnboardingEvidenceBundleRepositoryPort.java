package io.attestry.userauth.application.port;

import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceBundle;
import java.util.Optional;

public interface OnboardingEvidenceBundleRepositoryPort {
    OnboardingEvidenceBundle save(OnboardingEvidenceBundle bundle);

    Optional<OnboardingEvidenceBundle> findById(String evidenceBundleId);
}
