package io.attestry.workflow.interfaces.servicerequest;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.servicerequest.command.CompleteServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.command.GrantServiceConsentCommand;
import io.attestry.workflow.application.servicerequest.command.SubmitServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.CancelServiceRequestResult;
import io.attestry.workflow.application.servicerequest.result.CompleteServiceRequestResult;
import io.attestry.workflow.application.servicerequest.result.GrantServiceConsentResult;
import io.attestry.workflow.application.servicerequest.result.RevokeServiceConsentResult;
import io.attestry.workflow.application.servicerequest.result.SubmitServiceRequestResult;
import io.attestry.workflow.application.shipment.command.CompleteShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.PresignShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.result.PresignedShipmentEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.ShipmentEvidenceCompleteResult;
import io.attestry.workflow.application.usecase.ServiceCancelUseCase;
import io.attestry.workflow.application.usecase.ServiceCompleteUseCase;
import io.attestry.workflow.application.usecase.ServiceConsentUseCase;
import io.attestry.workflow.application.usecase.ServiceEvidenceUseCase;
import io.attestry.workflow.application.usecase.ServiceSubmitUseCase;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflows")
public class ServiceRequestHttp {

    private final ServiceSubmitUseCase serviceSubmitUseCase;
    private final ServiceCompleteUseCase serviceCompleteUseCase;
    private final ServiceCancelUseCase serviceCancelUseCase;
    private final ServiceEvidenceUseCase serviceEvidenceUseCase;
    private final ServiceConsentUseCase serviceConsentUseCase;

    public ServiceRequestHttp(
        ServiceSubmitUseCase serviceSubmitUseCase,
        ServiceCompleteUseCase serviceCompleteUseCase,
        ServiceCancelUseCase serviceCancelUseCase,
        ServiceEvidenceUseCase serviceEvidenceUseCase,
        ServiceConsentUseCase serviceConsentUseCase
    ) {
        this.serviceSubmitUseCase = serviceSubmitUseCase;
        this.serviceCompleteUseCase = serviceCompleteUseCase;
        this.serviceCancelUseCase = serviceCancelUseCase;
        this.serviceEvidenceUseCase = serviceEvidenceUseCase;
        this.serviceConsentUseCase = serviceConsentUseCase;
    }

    // --- Consent endpoints (OWNER) ---

    @PostMapping("/passports/{passportId}/service-consent")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_OWNER_SERVICE_CREATE')")
    public GrantServiceConsentResponse grantConsent(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("passportId") String passportId,
        @RequestBody GrantServiceConsentRequest request
    ) {
        GrantServiceConsentResult result = serviceConsentUseCase.grantConsent(
            principal,
            passportId,
            new GrantServiceConsentCommand(request.providerTenantId())
        );
        return GrantServiceConsentResponse.from(result);
    }

    @PostMapping("/passports/{passportId}/service-consent/revoke")
    @PreAuthorize("hasAuthority('SCOPE_OWNER_SERVICE_CREATE')")
    public RevokeServiceConsentResponse revokeConsent(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("passportId") String passportId,
        @RequestBody RevokeServiceConsentRequest request
    ) {
        RevokeServiceConsentResult result = serviceConsentUseCase.revokeConsent(
            principal,
            passportId,
            request.providerTenantId()
        );
        return RevokeServiceConsentResponse.from(result);
    }

    // --- Submit endpoint (PROVIDER) ---

    @PostMapping("/tenants/{tenantId}/service-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_SERVICE_COMPLETE')")
    public SubmitServiceRequestResponse submit(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @RequestBody SubmitServiceRequestRequest request
    ) {
        SubmitServiceRequestResult result = serviceSubmitUseCase.submit(
            principal,
            tenantId,
            new SubmitServiceRequestCommand(
                request.passportId(),
                request.serviceType(),
                request.description(),
                request.beforeEvidenceGroupId()
            )
        );
        return SubmitServiceRequestResponse.from(result);
    }

    // --- Complete endpoint (PROVIDER) ---

    @PostMapping("/tenants/{tenantId}/service-requests/{serviceRequestId}/complete")
    @PreAuthorize("hasAuthority('SCOPE_SERVICE_COMPLETE')")
    public CompleteServiceRequestResponse complete(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("serviceRequestId") String serviceRequestId,
        @RequestBody CompleteServiceRequestRequest request
    ) {
        CompleteServiceRequestResult result = serviceCompleteUseCase.complete(
            principal,
            tenantId,
            serviceRequestId,
            new CompleteServiceRequestCommand(
                request.afterEvidenceGroupId(),
                request.serviceResult()
            )
        );
        return CompleteServiceRequestResponse.from(result);
    }

    // --- Cancel endpoint (OWNER) ---

    @PostMapping("/service-requests/{serviceRequestId}/cancel")
    @PreAuthorize("hasAuthority('SCOPE_OWNER_SERVICE_CREATE')")
    public CancelServiceRequestResponse cancel(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("serviceRequestId") String serviceRequestId,
        @RequestBody(required = false) CancelServiceRequestRequest request
    ) {
        CancelServiceRequestResult result = serviceCancelUseCase.cancel(
            principal,
            serviceRequestId,
            request == null ? null : request.cancelReason()
        );
        return CancelServiceRequestResponse.from(result);
    }

    // --- Evidence endpoints (OWNER) ---

    @PostMapping("/service-requests/evidences/presign")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_OWNER_SERVICE_CREATE')")
    public PresignEvidenceResponse presignOwnerEvidence(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody PresignEvidenceRequest request
    ) {
        PresignedShipmentEvidenceUploadResult result = serviceEvidenceUseCase.presignOwnerEvidenceUpload(
            principal,
            new PresignShipmentEvidenceUploadCommand(
                request.evidenceGroupId(),
                request.fileName(),
                request.contentType()
            )
        );
        return PresignEvidenceResponse.from(result);
    }

    @PostMapping("/service-requests/evidences/complete")
    @PreAuthorize("hasAuthority('SCOPE_OWNER_SERVICE_CREATE')")
    public CompleteEvidenceResponse completeOwnerEvidence(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody CompleteEvidenceRequest request
    ) {
        ShipmentEvidenceCompleteResult result = serviceEvidenceUseCase.completeOwnerEvidenceUpload(
            principal,
            new CompleteShipmentEvidenceUploadCommand(
                request.evidenceGroupId(),
                request.evidenceId(),
                request.sizeBytes(),
                request.fileHash()
            )
        );
        return CompleteEvidenceResponse.from(result);
    }

    // --- Evidence endpoints (PROVIDER) ---

    @PostMapping("/tenants/{tenantId}/service-requests/evidences/presign")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_SERVICE_COMPLETE')")
    public PresignEvidenceResponse presignProviderEvidence(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @RequestBody PresignEvidenceRequest request
    ) {
        PresignedShipmentEvidenceUploadResult result = serviceEvidenceUseCase.presignEvidenceUpload(
            principal,
            tenantId,
            new PresignShipmentEvidenceUploadCommand(
                request.evidenceGroupId(),
                request.fileName(),
                request.contentType()
            )
        );
        return PresignEvidenceResponse.from(result);
    }

    @PostMapping("/tenants/{tenantId}/service-requests/evidences/complete")
    @PreAuthorize("hasAuthority('SCOPE_SERVICE_COMPLETE')")
    public CompleteEvidenceResponse completeProviderEvidence(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @RequestBody CompleteEvidenceRequest request
    ) {
        ShipmentEvidenceCompleteResult result = serviceEvidenceUseCase.completeEvidenceUpload(
            principal,
            tenantId,
            new CompleteShipmentEvidenceUploadCommand(
                request.evidenceGroupId(),
                request.evidenceId(),
                request.sizeBytes(),
                request.fileHash()
            )
        );
        return CompleteEvidenceResponse.from(result);
    }

    // --- Request/Response records ---

    public record GrantServiceConsentRequest(String providerTenantId) {
    }

    public record GrantServiceConsentResponse(
        String permissionId,
        String passportId,
        String providerTenantId,
        String status,
        Instant grantedAt
    ) {
        static GrantServiceConsentResponse from(GrantServiceConsentResult result) {
            return new GrantServiceConsentResponse(
                result.permissionId(),
                result.passportId(),
                result.providerTenantId(),
                result.status(),
                result.grantedAt()
            );
        }
    }

    public record RevokeServiceConsentRequest(String providerTenantId) {
    }

    public record RevokeServiceConsentResponse(
        String passportId,
        String providerTenantId,
        String status
    ) {
        static RevokeServiceConsentResponse from(RevokeServiceConsentResult result) {
            return new RevokeServiceConsentResponse(
                result.passportId(),
                result.providerTenantId(),
                result.status()
            );
        }
    }

    public record SubmitServiceRequestRequest(
        String passportId,
        String serviceType,
        String description,
        String beforeEvidenceGroupId
    ) {
    }

    public record CompleteServiceRequestRequest(
        String afterEvidenceGroupId,
        String serviceResult
    ) {
    }

    public record CancelServiceRequestRequest(String cancelReason) {
    }

    public record PresignEvidenceRequest(
        String evidenceGroupId,
        String fileName,
        String contentType
    ) {
    }

    public record CompleteEvidenceRequest(
        String evidenceGroupId,
        String evidenceId,
        long sizeBytes,
        String fileHash
    ) {
    }

    public record SubmitServiceRequestResponse(
        String serviceRequestId,
        String passportId,
        String serviceType,
        String status,
        String permissionId,
        Instant submittedAt
    ) {
        static SubmitServiceRequestResponse from(SubmitServiceRequestResult result) {
            return new SubmitServiceRequestResponse(
                result.serviceRequestId(),
                result.passportId(),
                result.serviceType(),
                result.status(),
                result.permissionId(),
                result.submittedAt()
            );
        }
    }

    public record CompleteServiceRequestResponse(
        String serviceRequestId,
        String passportId,
        String status,
        Instant completedAt,
        String outboxEventId
    ) {
        static CompleteServiceRequestResponse from(CompleteServiceRequestResult result) {
            return new CompleteServiceRequestResponse(
                result.serviceRequestId(),
                result.passportId(),
                result.status(),
                result.completedAt(),
                result.outboxEventId()
            );
        }
    }

    public record CancelServiceRequestResponse(
        String serviceRequestId,
        String passportId,
        String status,
        Instant cancelledAt
    ) {
        static CancelServiceRequestResponse from(CancelServiceRequestResult result) {
            return new CancelServiceRequestResponse(
                result.serviceRequestId(),
                result.passportId(),
                result.status(),
                result.cancelledAt()
            );
        }
    }

    public record PresignEvidenceResponse(
        String evidenceGroupId,
        String evidenceId,
        String objectKey,
        String uploadUrl,
        Instant expiresAt
    ) {
        static PresignEvidenceResponse from(PresignedShipmentEvidenceUploadResult result) {
            return new PresignEvidenceResponse(
                result.evidenceGroupId(),
                result.evidenceId(),
                result.objectKey(),
                result.uploadUrl(),
                result.expiresAt()
            );
        }
    }

    public record CompleteEvidenceResponse(
        String evidenceGroupId,
        String evidenceId,
        String status
    ) {
        static CompleteEvidenceResponse from(ShipmentEvidenceCompleteResult result) {
            return new CompleteEvidenceResponse(result.evidenceGroupId(), result.evidenceId(), result.status());
        }
    }
}
