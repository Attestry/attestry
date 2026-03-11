package io.attestry.userauth.application.port.onboarding;

import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceBundle;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceFile;
import java.util.List;
import java.util.Optional;

public interface OnboardingEvidenceBundleRepositoryPort {
    OnboardingEvidenceBundle save(OnboardingEvidenceBundle bundle);

    Optional<OnboardingEvidenceBundle> findById(String evidenceBundleId);

    List<OnboardingEvidenceBundle> findByIds(List<String> evidenceBundleIds);

    Optional<OnboardingEvidenceFile> findFileById(String evidenceFileId);

}
