package io.attestry.userauth.application.usecase.onboarding;

import io.attestry.userauth.application.dto.command.CompleteEvidenceUploadCommand;
import io.attestry.userauth.application.dto.command.CreateBrandApplicationCommand;
import io.attestry.userauth.application.dto.command.CreateRetailApplicationCommand;
import io.attestry.userauth.application.dto.command.PresignEvidenceUploadCommand;
import io.attestry.userauth.application.dto.result.ApplicationResult;
import io.attestry.userauth.application.dto.result.ApproveApplicationResult;
import io.attestry.userauth.application.dto.result.EvidenceBundleResult;
import io.attestry.userauth.application.dto.result.PresignedEvidenceUploadResult;
import io.attestry.userauth.application.dto.view.ApplicationView;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import java.util.List;

public interface OnboardingUseCase {
    ApplicationResult createBrandApplication(AuthPrincipal principal, CreateBrandApplicationCommand command);

    ApplicationResult createRetailApplication(AuthPrincipal principal, CreateRetailApplicationCommand command);

    List<ApplicationView> listBrandApplications();

    ApplicationView getBrandApplication(String applicationId);

    ApproveApplicationResult approveBrandApplication(AuthPrincipal principal, String applicationId);

    ApplicationResult rejectBrandApplication(AuthPrincipal principal, String applicationId, String rejectReason);

    ApplicationView getRetailApplication(String applicationId);

    List<ApplicationView> listRetailApplications();

    ApproveApplicationResult approveRetailApplication(AuthPrincipal principal, String applicationId);

    ApplicationResult rejectRetailApplication(AuthPrincipal principal, String applicationId, String rejectReason);

    PresignedEvidenceUploadResult presignEvidenceUpload(AuthPrincipal principal, PresignEvidenceUploadCommand command);

    EvidenceBundleResult completeEvidenceUpload(AuthPrincipal principal, CompleteEvidenceUploadCommand command);

}
