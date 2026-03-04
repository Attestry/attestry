package io.attestry.userauth.interfaces.onboarding;

import io.attestry.userauth.application.dto.command.CompleteEvidenceUploadCommand;
import io.attestry.userauth.application.dto.command.CreateApplicationCommand;
import io.attestry.userauth.application.dto.command.PresignEvidenceUploadCommand;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.result.ApplicationResult;
import io.attestry.userauth.application.usecase.onboarding.OnboardingUseCase;
import io.attestry.userauth.security.CurrentActor;
import io.attestry.userauth.interfaces.onboarding.dto.request.CreateApplicationRequest;
import io.attestry.userauth.interfaces.onboarding.dto.request.CompleteEvidenceUploadRequest;
import io.attestry.userauth.interfaces.onboarding.dto.request.PresignEvidenceUploadRequest;
import io.attestry.userauth.interfaces.onboarding.dto.response.ApplicationResponse;
import io.attestry.userauth.interfaces.onboarding.dto.response.EvidenceBundleResponse;
import io.attestry.userauth.interfaces.onboarding.dto.response.PresignedEvidenceUploadResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
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
        @CurrentActor ActorContext actor,
        @RequestBody PresignEvidenceUploadRequest request
    ) {
        return PresignedEvidenceUploadResponse.from(
            onboardingService.presignEvidenceUpload(
                actor,
                new PresignEvidenceUploadCommand(request.evidenceBundleId(), request.fileName(), request.contentType())
            )
        );
    }

    @PostMapping("/onboarding/evidences/complete")
    public EvidenceBundleResponse completeEvidenceUpload(
        @CurrentActor ActorContext actor,
        @RequestBody CompleteEvidenceUploadRequest request
    ) {
        return EvidenceBundleResponse.from(
            onboardingService.completeEvidenceUpload(
                actor,
                new CompleteEvidenceUploadCommand(request.evidenceBundleId(), request.evidenceFileId(), request.sizeBytes())
            )
        );
    }

    @PostMapping("/onboarding/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse createApplication(
        @CurrentActor ActorContext actor,
        @RequestBody CreateApplicationRequest request
    ) {
        ApplicationResult result = onboardingService.createApplication(
            actor,
            new CreateApplicationCommand(
                request.type(),
                request.orgName(),
                request.country(),
                request.bizRegNo(),
                request.evidenceBundleId()
            )
        );
        return ApplicationResponse.from(result);
    }

    //TODO("my-page 에 넣어야 되나?")
    @GetMapping("/onboarding/applications")
    public List<ApplicationResponse> listMyApplications(@CurrentActor ActorContext actor) {
        return onboardingService.listMyApplications(actor).stream()
            .map(ApplicationResponse::from)
            .toList();
    }

    //TODO("my-page 에 넣어야 되나?")
    @GetMapping("/onboarding/applications/{applicationId}")
    public ApplicationResponse getMyApplication(
        @CurrentActor ActorContext actor,
        @PathVariable(name = "applicationId") String applicationId
    ) {
        return ApplicationResponse.from(onboardingService.getMyApplication(actor, applicationId));
    }
}
