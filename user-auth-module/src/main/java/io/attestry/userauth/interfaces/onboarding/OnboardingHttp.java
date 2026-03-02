package io.attestry.userauth.interfaces.onboarding;

import io.attestry.userauth.application.dto.command.CompleteEvidenceUploadCommand;
import io.attestry.userauth.application.dto.command.CreateBrandApplicationCommand;
import io.attestry.userauth.application.dto.command.CreateRetailApplicationCommand;
import io.attestry.userauth.application.dto.command.PresignEvidenceUploadCommand;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.result.ApplicationResult;
import io.attestry.userauth.application.usecase.onboarding.OnboardingUseCase;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.interfaces.onboarding.dto.request.CompleteEvidenceUploadRequest;
import io.attestry.userauth.interfaces.onboarding.dto.request.CreateBrandApplicationRequest;
import io.attestry.userauth.interfaces.onboarding.dto.request.CreateRetailApplicationRequest;
import io.attestry.userauth.interfaces.onboarding.dto.request.PresignEvidenceUploadRequest;
import io.attestry.userauth.interfaces.onboarding.dto.response.ApplicationResponse;
import io.attestry.userauth.interfaces.onboarding.dto.response.EvidenceBundleResponse;
import io.attestry.userauth.interfaces.onboarding.dto.response.PresignedEvidenceUploadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class OnboardingHttp {

    private final OnboardingUseCase onboardingService;

    public OnboardingHttp(OnboardingUseCase onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/onboarding/evidences/presign")
    @ResponseStatus(HttpStatus.CREATED)
    public PresignedEvidenceUploadResponse presignEvidenceUpload(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody PresignEvidenceUploadRequest request
    ) {
        return PresignedEvidenceUploadResponse.from(
            onboardingService.presignEvidenceUpload(
                toActorContext(principal),
                new PresignEvidenceUploadCommand(request.evidenceBundleId(), request.fileName(), request.contentType())
            )
        );
    }

    @PostMapping("/onboarding/evidences/complete")
    public EvidenceBundleResponse completeEvidenceUpload(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody CompleteEvidenceUploadRequest request
    ) {
        return EvidenceBundleResponse.from(
            onboardingService.completeEvidenceUpload(
                toActorContext(principal),
                new CompleteEvidenceUploadCommand(request.evidenceBundleId(), request.evidenceFileId(), request.sizeBytes())
            )
        );
    }

    @PostMapping("/brand-applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse createBrandApplication(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody CreateBrandApplicationRequest request
    ) {
        ApplicationResult result = onboardingService.createBrandApplication(
            toActorContext(principal),
            new CreateBrandApplicationCommand(
                request.brandName(),
                request.country(),
                request.bizRegNo(),
                request.evidenceBundleId()
            )
        );
        return ApplicationResponse.from(result);
    }

    @GetMapping("/brand-applications/{applicationId}")
    public ApplicationResponse getBrandApplication(@PathVariable(name = "applicationId") String applicationId) {
        return ApplicationResponse.from(onboardingService.getBrandApplication(applicationId));
    }

    @PostMapping("/retail-applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse createRetailApplication(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody CreateRetailApplicationRequest request
    ) {
        ApplicationResult result = onboardingService.createRetailApplication(
            toActorContext(principal),
            new CreateRetailApplicationCommand(
                request.retailName(),
                request.country(),
                request.bizRegNo(),
                request.evidenceBundleId()
            )
        );
        return ApplicationResponse.from(result);
    }

    @GetMapping("/retail-applications/{id}")
    public ApplicationResponse getRetailApplication(@PathVariable("id") String id) {
        return ApplicationResponse.from(onboardingService.getRetailApplication(id));
    }

    private ActorContext toActorContext(AuthPrincipal principal) {
        return new ActorContext(
            principal.tokenId(),
            principal.userId(),
            principal.tenantId(),
            principal.groupId(),
            principal.verificationLevel(),
            principal.scopes(),
            principal.expiresAt()
        );
    }
}
