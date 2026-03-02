package io.attestry.userauth.application.usecase.onboarding;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.CompleteEvidenceUploadCommand;
import io.attestry.userauth.application.dto.command.CreateBrandApplicationCommand;
import io.attestry.userauth.application.dto.command.CreateRetailApplicationCommand;
import io.attestry.userauth.application.dto.command.PresignEvidenceUploadCommand;
import io.attestry.userauth.application.dto.result.ApplicationResult;
import io.attestry.userauth.application.dto.result.ApproveApplicationResult;
import io.attestry.userauth.application.dto.result.EvidenceBundleResult;
import io.attestry.userauth.application.dto.result.PresignedEvidenceUploadResult;
import io.attestry.userauth.application.dto.view.ApplicationView;
import java.util.List;

public interface OnboardingUseCase {
    ApplicationResult createBrandApplication(ActorContext actor, CreateBrandApplicationCommand command);

    ApplicationResult createRetailApplication(ActorContext actor, CreateRetailApplicationCommand command);

    List<ApplicationView> listBrandApplications();

    ApplicationView getBrandApplication(String applicationId);

    ApproveApplicationResult approveBrandApplication(ActorContext actor, String applicationId);

    ApplicationResult rejectBrandApplication(ActorContext actor, String applicationId, String rejectReason);

    ApplicationView getRetailApplication(String applicationId);

    List<ApplicationView> listRetailApplications();

    ApproveApplicationResult approveRetailApplication(ActorContext actor, String applicationId);

    ApplicationResult rejectRetailApplication(ActorContext actor, String applicationId, String rejectReason);

    PresignedEvidenceUploadResult presignEvidenceUpload(ActorContext actor, PresignEvidenceUploadCommand command);

    EvidenceBundleResult completeEvidenceUpload(ActorContext actor, CompleteEvidenceUploadCommand command);

}
