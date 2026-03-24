package io.attestry.userauth.application.onboarding.command;

import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.onboarding.result.EvidenceBundleResult;
import io.attestry.userauth.application.port.onboarding.OnboardingEvidenceBundleRepositoryPort;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceBundle;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingEvidenceUploadCompletionTxService {

    private final OnboardingEvidenceBundleRepositoryPort evidenceBundleRepository;
    private final Clock clock;

    @Transactional
    public EvidenceBundleResult completeEvidenceUpload(ActorContext actor, CompleteEvidenceUploadCommand command) {
        OnboardingEvidenceBundle bundle = evidenceBundleRepository.findById(command.evidenceBundleId())
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.EVIDENCE_NOT_FOUND, "Evidence not found"));
        bundle.assertOwnedBy(actor.userId());

        bundle.completeFile(command.evidenceFileId(), command.sizeBytes(), Instant.now(clock));
        evidenceBundleRepository.save(bundle);

        return new EvidenceBundleResult(bundle.evidenceBundleId(), bundle.status().name());
    }
}
