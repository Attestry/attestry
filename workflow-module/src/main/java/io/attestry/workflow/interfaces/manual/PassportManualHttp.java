package io.attestry.workflow.interfaces.manual;

import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.manual.command.SendPassportManualCommand;
import io.attestry.workflow.application.manual.result.PassportManualRecipientResult;
import io.attestry.workflow.application.manual.result.SendPassportManualResult;
import io.attestry.workflow.application.manual.usecase.PassportManualEvidenceUseCase;
import io.attestry.workflow.application.manual.usecase.PassportManualUseCase;
import io.attestry.workflow.application.shipment.command.CompleteShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.PresignShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.result.EvidenceCompleteResult;
import io.attestry.workflow.application.shipment.result.PresignedEvidenceUploadResult;
import io.attestry.workflow.interfaces.manual.dto.request.CompletePassportManualEvidenceRequest;
import io.attestry.workflow.interfaces.manual.dto.request.PresignPassportManualEvidenceRequest;
import io.attestry.workflow.interfaces.manual.dto.request.SendPassportManualRequest;
import io.attestry.workflow.interfaces.manual.dto.response.CompletePassportManualEvidenceResponse;
import io.attestry.workflow.interfaces.manual.dto.response.PassportManualRecipientResponse;
import io.attestry.workflow.interfaces.manual.dto.response.PresignPassportManualEvidenceResponse;
import io.attestry.workflow.interfaces.manual.dto.response.SendPassportManualResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/workflows")
public class PassportManualHttp {

    private final PassportManualUseCase passportManualUseCase;
    private final PassportManualEvidenceUseCase passportManualEvidenceUseCase;

    @GetMapping("/tenants/{tenantId}/passports/{passportId}/manual-delivery-recipient")
    @PreAuthorize("hasAnyAuthority('SCOPE_BRAND_RELEASE', 'SCOPE_TENANT_READ_ONLY')")
    public ApiResponse<PassportManualRecipientResponse> getRecipient(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("passportId") String passportId
    ) {
        PassportManualRecipientResult result = passportManualUseCase.getRecipient(actor(principal), tenantId, passportId);
        return ApiResponse.success(PassportManualRecipientResponse.from(result));
    }

    @PostMapping("/tenants/{tenantId}/passport-manuals/manual-deliveries")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_BRAND_RELEASE')")
    public ApiResponse<SendPassportManualResponse> send(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @Valid @RequestBody SendPassportManualRequest request
    ) {
        SendPassportManualResult result = passportManualUseCase.send(
            actor(principal),
            tenantId,
            new SendPassportManualCommand(request.passportIds(), request.message(), request.evidenceGroupId())
        );
        return ApiResponse.success(SendPassportManualResponse.from(result));
    }

    @PostMapping("/tenants/{tenantId}/passport-manuals/evidences/presign")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_BRAND_RELEASE')")
    public ApiResponse<PresignPassportManualEvidenceResponse> presignEvidence(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @RequestBody PresignPassportManualEvidenceRequest request
    ) {
        PresignedEvidenceUploadResult result = passportManualEvidenceUseCase.presignEvidenceUpload(
            actor(principal),
            tenantId,
            new PresignShipmentEvidenceUploadCommand(
                request.evidenceGroupId(),
                request.fileName(),
                request.contentType()
            )
        );
        return ApiResponse.success(PresignPassportManualEvidenceResponse.from(result));
    }

    @PostMapping("/tenants/{tenantId}/passport-manuals/evidences/complete")
    @PreAuthorize("hasAuthority('SCOPE_BRAND_RELEASE')")
    public ApiResponse<CompletePassportManualEvidenceResponse> completeEvidence(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @RequestBody CompletePassportManualEvidenceRequest request
    ) {
        EvidenceCompleteResult result = passportManualEvidenceUseCase.completeEvidenceUpload(
            actor(principal),
            tenantId,
            new CompleteShipmentEvidenceUploadCommand(
                request.evidenceGroupId(),
                request.evidenceId(),
                request.sizeBytes(),
                request.fileHash()
            )
        );
        return ApiResponse.success(CompletePassportManualEvidenceResponse.from(result));
    }

    private WorkflowActorContext actor(AuthPrincipal principal) {
        return WorkflowActorContext.from(principal);
    }
}
