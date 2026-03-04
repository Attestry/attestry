package io.attestry.userauth.domain.onboarding.repository;

import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceBundle;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceFile;
import java.util.List;
import java.util.Optional;

public interface EvidenceBundleRepository {
    OnboardingEvidenceBundle save(OnboardingEvidenceBundle bundle);

    Optional<OnboardingEvidenceBundle> findById(String evidenceBundleId);

    OnboardingEvidenceFile saveFile(OnboardingEvidenceFile file);

    Optional<OnboardingEvidenceFile> findFileById(String evidenceFileId);

    List<OnboardingEvidenceFile> findFilesByBundleId(String evidenceBundleId);
}
