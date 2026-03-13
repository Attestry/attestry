package io.attestry.userauth.application.onboarding.command;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.CompleteEvidenceUploadCommand;
import io.attestry.userauth.application.dto.command.CreateApplicationCommand;
import io.attestry.userauth.application.dto.command.PresignEvidenceUploadCommand;
import io.attestry.userauth.application.dto.result.ApplicationResult;
import io.attestry.userauth.application.dto.result.ApproveApplicationResult;
import io.attestry.userauth.application.dto.result.EvidenceBundleResult;
import io.attestry.userauth.application.dto.result.PresignedEvidenceUploadResult;
import io.attestry.userauth.application.usecase.onboarding.OnboardingApplicationCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@RequiredArgsConstructor
@Service
public class OnboardingApplicationCommandService implements OnboardingApplicationCommandUseCase {

    private final OnboardingApplicationLifecycleExecutor applicationLifecycleExecutor;
    private final OnboardingEvidenceCommandExecutor evidenceCommandExecutor;

    @Override
    @Transactional
    public ApplicationResult createApplication(ActorContext actor, CreateApplicationCommand command) {
        return applicationLifecycleExecutor.createApplication(actor, command);
    }

    @Override
    @Transactional
    public ApproveApplicationResult approveApplication(ActorContext actor, String applicationId) {
        return applicationLifecycleExecutor.approveApplication(actor, applicationId);
    }

    @Override
    @Transactional
    public ApplicationResult rejectApplication(ActorContext actor, String applicationId, String rejectReason) {
        return applicationLifecycleExecutor.rejectApplication(actor, applicationId, rejectReason);
    }

    @Override
    @Transactional
    public PresignedEvidenceUploadResult presignEvidenceUpload(ActorContext actor, PresignEvidenceUploadCommand command) {
        return evidenceCommandExecutor.presignEvidenceUpload(actor, command);
    }

    @Override
    public EvidenceBundleResult completeEvidenceUpload(ActorContext actor, CompleteEvidenceUploadCommand command) {
        return evidenceCommandExecutor.completeEvidenceUpload(actor, command);
    }
}
