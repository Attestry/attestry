package io.attestry.userauth.application.port;

import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceFile;
import java.util.List;
import java.util.Optional;

public interface OnboardingEvidenceFileRepositoryPort {
    OnboardingEvidenceFile save(OnboardingEvidenceFile file);

    Optional<OnboardingEvidenceFile> findById(String evidenceFileId);

    List<OnboardingEvidenceFile> findByBundleId(String evidenceBundleId);
}
