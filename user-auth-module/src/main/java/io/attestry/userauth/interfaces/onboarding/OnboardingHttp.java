package io.attestry.userauth.interfaces.onboarding;

import io.attestry.commonlib.web.CurrentActor;
import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.onboarding.command.CompleteEvidenceUploadCommand;
import io.attestry.userauth.application.onboarding.command.CreateApplicationCommand;
import io.attestry.userauth.application.onboarding.command.OnboardingApplicationCommandUseCase;
import io.attestry.userauth.application.onboarding.command.PresignEvidenceUploadCommand;
import io.attestry.userauth.application.onboarding.query.OnboardingApplicationQueryUseCase;
import io.attestry.userauth.application.onboarding.result.ApplicationResult;
import io.attestry.userauth.application.onboarding.result.ApproveApplicationResult;
import io.attestry.userauth.interfaces.onboarding.dto.request.CompleteEvidenceUploadRequest;
import io.attestry.userauth.interfaces.onboarding.dto.request.CreateApplicationRequest;
import io.attestry.userauth.interfaces.onboarding.dto.request.PresignEvidenceUploadRequest;
import io.attestry.userauth.interfaces.onboarding.dto.request.RejectApplicationRequest;
import io.attestry.userauth.interfaces.onboarding.dto.response.ApplicationResponse;
import io.attestry.userauth.interfaces.onboarding.dto.response.ApproveResponse;
import io.attestry.userauth.interfaces.onboarding.dto.response.EvidenceBundleResponse;
import io.attestry.userauth.interfaces.onboarding.dto.response.PresignedEvidenceUploadResponse;
import jakarta.validation.Valid;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/onboarding")
public class OnboardingHttp {

    private final OnboardingApplicationCommandUseCase onboardingCommandUseCase;
    private final OnboardingApplicationQueryUseCase onboardingQueryUseCase;


    @PostMapping("/evidences/presign")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PresignedEvidenceUploadResponse> presignEvidenceUpload(
        @CurrentActor ActorContext actor,
        @Valid @RequestBody PresignEvidenceUploadRequest request
    ) {
        return ApiResponse.success(PresignedEvidenceUploadResponse.from(
            onboardingCommandUseCase.presignEvidenceUpload(
                actor,
                new PresignEvidenceUploadCommand(request.evidenceBundleId(), request.fileName(), request.contentType())
            )
        ));
    }

    @PostMapping("/evidences/complete")
    public ApiResponse<EvidenceBundleResponse> completeEvidenceUpload(
        @CurrentActor ActorContext actor,
        @Valid @RequestBody CompleteEvidenceUploadRequest request
    ) {
        return ApiResponse.success(EvidenceBundleResponse.from(
            onboardingCommandUseCase.completeEvidenceUpload(
                actor,
                new CompleteEvidenceUploadCommand(request.evidenceBundleId(), request.evidenceFileId(), request.sizeBytes())
            )
        ));
    }

    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApplicationResponse> createApplication(
        @CurrentActor ActorContext actor,
        @Valid @RequestBody CreateApplicationRequest request
    ) {
        ApplicationResult result = onboardingCommandUseCase.createApplication(
            actor,
            new CreateApplicationCommand(
                request.type(),
                request.orgName(),
                request.country(),
                request.address(),
                request.bizRegNo(),
                request.evidenceBundleId()
            )
        );
        return ApiResponse.success(ApplicationResponse.from(result));
    }

    // TODO("엔드포인트 수정 -> 기존 - Path: `/onboarding/applications`)
    @GetMapping("/applications/me")
    public ApiResponse<List<ApplicationResponse>> listMyApplications(@CurrentActor ActorContext actor) {
        return ApiResponse.success(onboardingQueryUseCase.listMyApplications(actor).stream()
            .map(ApplicationResponse::from)
            .toList());
    }

    @GetMapping("/applications/me/{applicationId}")
    public ApiResponse<ApplicationResponse> getMyApplication(
            @CurrentActor ActorContext actor,
            @PathVariable(name = "applicationId") String applicationId
    ) {
        return ApiResponse.success(
                ApplicationResponse.from(onboardingQueryUseCase.getMyApplication(actor, applicationId))
        );
    }

    @GetMapping("/applications/{applicationId}")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public ApiResponse<ApplicationResponse> getApplication(
            @PathVariable(name = "applicationId") String applicationId
    ) {
        return ApiResponse.success(
                ApplicationResponse.from(onboardingQueryUseCase.getApplication(applicationId))
        );
    }

    @GetMapping("/applications")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public ApiResponse<List<ApplicationResponse>> listApplications(@RequestParam(name = "type", required = false) String type) {
        return ApiResponse.success(onboardingQueryUseCase.listApplications(type).stream()
                .map(ApplicationResponse::from)
                .toList());
    }

    // TODO("기존 : `/admin/onboarding/applications/{applicationId}`")
    @PostMapping("/applications/{applicationId}/approve")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public ApiResponse<ApproveResponse> approveApplication(
            @CurrentActor ActorContext actor,
            @PathVariable(name = "applicationId") String applicationId
    ) {
        ApproveApplicationResult result = onboardingCommandUseCase.approveApplication(actor, applicationId);
        return ApiResponse.success(new ApproveResponse(result.tenantId(), result.membershipId()));
    }

    //TODO("기존 -> `/admin/onboarding/applications/{applicationId}/reject`")
    @PostMapping("/applications/{applicationId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public void rejectApplication(
            @CurrentActor ActorContext actor,
            @PathVariable(name = "applicationId") String applicationId,
            @Valid @RequestBody RejectApplicationRequest request
    ) {
        onboardingCommandUseCase.rejectApplication(actor, applicationId, request.reason());
    }
}
