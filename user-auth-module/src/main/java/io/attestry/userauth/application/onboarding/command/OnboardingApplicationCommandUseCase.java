package io.attestry.userauth.application.onboarding.command;

import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.onboarding.result.ApplicationResult;
import io.attestry.userauth.application.onboarding.result.ApproveApplicationResult;
import io.attestry.userauth.application.onboarding.result.EvidenceBundleResult;
import io.attestry.userauth.application.onboarding.result.PresignedEvidenceUploadResult;

public interface OnboardingApplicationCommandUseCase {

    ApplicationResult createApplication(ActorContext actor, CreateApplicationCommand command);

    ApproveApplicationResult approveApplication(ActorContext actor, String applicationId);

    ApplicationResult rejectApplication(ActorContext actor, String applicationId, String rejectReason);

    PresignedEvidenceUploadResult presignEvidenceUpload(ActorContext actor, PresignEvidenceUploadCommand command);

    EvidenceBundleResult completeEvidenceUpload(ActorContext actor, CompleteEvidenceUploadCommand command);
}
