package io.attestry.userauth.application.usecase.onboarding;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.CompleteEvidenceUploadCommand;
import io.attestry.userauth.application.dto.command.CreateApplicationCommand;
import io.attestry.userauth.application.dto.command.PresignEvidenceUploadCommand;
import io.attestry.userauth.application.dto.result.ApplicationResult;
import io.attestry.userauth.application.dto.result.ApproveApplicationResult;
import io.attestry.userauth.application.dto.result.EvidenceBundleResult;
import io.attestry.userauth.application.dto.result.PresignedEvidenceUploadResult;

public interface OnboardingApplicationCommandUseCase {

    ApplicationResult createApplication(ActorContext actor, CreateApplicationCommand command);

    ApproveApplicationResult approveApplication(ActorContext actor, String applicationId);

    ApplicationResult rejectApplication(ActorContext actor, String applicationId, String rejectReason);

    PresignedEvidenceUploadResult presignEvidenceUpload(ActorContext actor, PresignEvidenceUploadCommand command);

    EvidenceBundleResult completeEvidenceUpload(ActorContext actor, CompleteEvidenceUploadCommand command);
}
