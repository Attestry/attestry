package io.attestry.userauth.application.onboarding;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.CompleteEvidenceUploadCommand;
import io.attestry.userauth.application.dto.result.EvidenceBundleResult;
import io.attestry.userauth.application.port.OnboardingEvidenceBundleRepositoryPort;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceBundle;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingEvidenceUploadCompletionTxService {

    private final OnboardingEvidenceBundleRepositoryPort evidenceBundleRepository;
    private final Clock clock;

    public OnboardingEvidenceUploadCompletionTxService(
        OnboardingEvidenceBundleRepositoryPort evidenceBundleRepository,
        Clock clock
    ) {
        this.evidenceBundleRepository = evidenceBundleRepository;
        this.clock = clock;
    }

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
