package io.attestry.workflow.interfaces.servicerequest;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.servicerequest.command.AcceptServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.command.CompleteServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.command.GrantServiceConsentCommand;
import io.attestry.workflow.application.servicerequest.command.RejectServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.command.SubmitServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.AcceptServiceRequestResult;
import io.attestry.workflow.application.servicerequest.result.CancelServiceRequestResult;
import io.attestry.workflow.application.servicerequest.result.CompleteServiceRequestResult;
import io.attestry.workflow.application.servicerequest.result.GrantServiceConsentResult;
import io.attestry.workflow.application.servicerequest.result.RejectServiceRequestResult;
import io.attestry.workflow.application.servicerequest.result.RevokeServiceConsentResult;
import io.attestry.workflow.application.servicerequest.result.SubmitServiceRequestResult;
import io.attestry.workflow.application.shipment.command.CompleteShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.PresignShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.result.EvidenceCompleteResult;
import io.attestry.workflow.application.shipment.result.PresignedEvidenceUploadResult;
import io.attestry.workflow.application.usecase.ServiceAcceptUseCase;
import io.attestry.workflow.application.usecase.ServiceCancelUseCase;
import io.attestry.workflow.application.usecase.ServiceCompleteUseCase;
import io.attestry.workflow.application.usecase.ServiceConsentUseCase;
import io.attestry.workflow.application.usecase.ServiceEvidenceUseCase;
import io.attestry.workflow.application.usecase.ServiceRejectUseCase;
import io.attestry.workflow.application.usecase.ServiceRequestQueryUseCase;
import io.attestry.workflow.application.usecase.ServiceSubmitUseCase;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflows")
public class ServiceRequestHttp {

    private final ServiceSubmitUseCase serviceSubmitUseCase;
    private final ServiceRequestQueryUseCase serviceRequestQueryUseCase;
    private final ServiceAcceptUseCase serviceAcceptUseCase;
    private final ServiceRejectUseCase serviceRejectUseCase;
    private final ServiceCompleteUseCase serviceCompleteUseCase;
    private final ServiceCancelUseCase serviceCancelUseCase;
    private final ServiceEvidenceUseCase serviceEvidenceUseCase;
    private final ServiceConsentUseCase serviceConsentUseCase;

    public ServiceRequestHttp(
        ServiceSubmitUseCase serviceSubmitUseCase,
        ServiceRequestQueryUseCase serviceRequestQueryUseCase,
        ServiceAcceptUseCase serviceAcceptUseCase,
        ServiceRejectUseCase serviceRejectUseCase,
        ServiceCompleteUseCase serviceCompleteUseCase,
        ServiceCancelUseCase serviceCancelUseCase,
        ServiceEvidenceUseCase serviceEvidenceUseCase,
        ServiceConsentUseCase serviceConsentUseCase
    ) {
        this.serviceSubmitUseCase = serviceSubmitUseCase;
        this.serviceRequestQueryUseCase = serviceRequestQueryUseCase;
        this.serviceAcceptUseCase = serviceAcceptUseCase;
        this.serviceRejectUseCase = serviceRejectUseCase;
        this.serviceCompleteUseCase = serviceCompleteUseCase;
        this.serviceCancelUseCase = serviceCancelUseCase;
        this.serviceEvidenceUseCase = serviceEvidenceUseCase;
        this.serviceConsentUseCase = serviceConsentUseCase;
    }

    @PostMapping("/passports/{passportId}/service-consent")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_OWNER_SERVICE_CREATE')")
    public GrantServiceConsentResponse submit(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("passportId") String passportId,
        @RequestBody GrantServiceConsentRequest request
    ) {
        GrantServiceConsentResult result = serviceConsentUseCase.submit(
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

    // TODO("region으로 검색가능하게끔, tenant 모듈로 분리")
    @GetMapping("/service/providers")
    public PagedServiceProviderResponse listServiceProviders(
        @RequestParam(name = "name", required = false) String name,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        ServiceConsentUseCase.PagedServiceProviderResult result = serviceConsentUseCase.listServiceProviders(
            name, page, size
        );
        List<ServiceProviderResponse> content = result.content().stream()
            .map(ServiceProviderResponse::from)
            .toList();
        return new PagedServiceProviderResponse(
            content,
            result.page(),
            result.size(),
            result.totalElements(),
            result.totalPages()
        );
    }

    // --- Submit endpoint (OWNER) ---

    @PostMapping("/service-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_OWNER_SERVICE_CREATE')")
    public SubmitServiceRequestResponse approve(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody SubmitServiceRequestRequest request
    ) {
        SubmitServiceRequestResult result = serviceSubmitUseCase.approve(
            principal,
            new SubmitServiceRequestCommand(
                request.passportId(),
                request.providerTenantId(),
                request.beforeEvidenceGroupId()
            )
        );
        return SubmitServiceRequestResponse.from(result);
    }

    @GetMapping("/service-requests/me")
    @PreAuthorize("hasAuthority('SCOPE_OWNER_SERVICE_CREATE')")
    public PagedServiceRequestResponse listMyRequests(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        ServiceRequestQueryUseCase.PagedServiceRequestResult result = serviceRequestQueryUseCase
            .listMyRequests(principal, status, page, size);
        return PagedServiceRequestResponse.from(result);
    }

    @GetMapping("/tenants/{tenantId}/service-requests")
    @PreAuthorize("hasAuthority('SCOPE_SERVICE_COMPLETE')")
    public PagedServiceRequestResponse listProviderRequests(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        ServiceRequestQueryUseCase.PagedServiceRequestResult result = serviceRequestQueryUseCase
            .listProviderRequests(principal, tenantId, status, page, size);
        return PagedServiceRequestResponse.from(result);
    }

    @PostMapping("/tenants/{tenantId}/service-requests/{serviceRequestId}/accept")
    @PreAuthorize("hasAuthority('SCOPE_SERVICE_COMPLETE')")
    public AcceptServiceRequestResponse accept(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("serviceRequestId") String serviceRequestId,
        @RequestBody AcceptServiceRequestRequest request
    ) {
        AcceptServiceRequestResult result = serviceAcceptUseCase.accept(
            principal,
            tenantId,
            serviceRequestId,
            new AcceptServiceRequestCommand(request.serviceType(), request.description())
        );
        return AcceptServiceRequestResponse.from(result);
    }

    @PostMapping("/tenants/{tenantId}/service-requests/{serviceRequestId}/reject")
    @PreAuthorize("hasAuthority('SCOPE_SERVICE_COMPLETE')")
    public RejectServiceRequestResponse reject(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("serviceRequestId") String serviceRequestId,
        @RequestBody(required = false) RejectServiceRequestRequest request
    ) {
        RejectServiceRequestResult result = serviceRejectUseCase.reject(
            principal,
            tenantId,
            serviceRequestId,
            new RejectServiceRequestCommand(request == null ? null : request.reason())
        );
        return RejectServiceRequestResponse.from(result);
    }

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

    @PostMapping("/service-requests/evidences/presign")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_OWNER_SERVICE_CREATE')")
    public PresignEvidenceResponse presignOwnerEvidence(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody PresignEvidenceRequest request
    ) {
        PresignedEvidenceUploadResult result = serviceEvidenceUseCase.presignOwnerEvidenceUpload(
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
        EvidenceCompleteResult result = serviceEvidenceUseCase.completeOwnerEvidenceUpload(
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

    @PostMapping("/tenants/{tenantId}/service-requests/evidences/presign")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_SERVICE_COMPLETE')")
    public PresignEvidenceResponse presignProviderEvidence(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @RequestBody PresignEvidenceRequest request
    ) {
        PresignedEvidenceUploadResult result = serviceEvidenceUseCase.presignEvidenceUpload(
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
        EvidenceCompleteResult result = serviceEvidenceUseCase.completeEvidenceUpload(
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

    public record GrantServiceConsentRequest(
        String providerTenantId
    ) {
    }

    public record GrantServiceConsentResponse(
        String permissionId,
        String serviceRequestId,
        String passportId,
        String providerTenantId,
        String consentStatus,
        String serviceRequestStatus,
        Instant grantedAt
    ) {
        static GrantServiceConsentResponse from(GrantServiceConsentResult result) {
            return new GrantServiceConsentResponse(
                result.permissionId(),
                result.serviceRequestId(),
                result.passportId(),
                result.providerTenantId(),
                result.consentStatus(),
                result.serviceRequestStatus(),
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

    public record ServiceProviderResponse(
        String tenantId,
        String name,
        String region,
        String type
    ) {
        static ServiceProviderResponse from(ServiceConsentUseCase.ServiceProviderResult result) {
            return new ServiceProviderResponse(
                result.tenantId(),
                result.name(),
                result.region(),
                result.type()
            );
        }
    }

    public record PagedServiceProviderResponse(
        List<ServiceProviderResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }

    public record SubmitServiceRequestRequest(
        String passportId,
        String providerTenantId,
        String beforeEvidenceGroupId
    ) {
    }

    public record AcceptServiceRequestRequest(
        String serviceType,
        String description
    ) {
    }

    public record CompleteServiceRequestRequest(
        String afterEvidenceGroupId,
        String serviceResult
    ) {
    }

    public record RejectServiceRequestRequest(String reason) {
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
        String providerTenantId,
        String serviceType,
        String status,
        String permissionId,
        Instant submittedAt
    ) {
        static SubmitServiceRequestResponse from(SubmitServiceRequestResult result) {
            return new SubmitServiceRequestResponse(
                result.serviceRequestId(),
                result.passportId(),
                result.providerTenantId(),
                result.serviceType(),
                result.status(),
                result.permissionId(),
                result.submittedAt()
            );
        }
    }

    public record ServiceRequestListItemResponse(
        String serviceRequestId,
        String passportId,
        String serialNumber,
        String modelName,
        String providerTenantId,
        String providerTenantName,
        String serviceType,
        String description,
        String status,
        Instant submittedAt
    ) {
        static ServiceRequestListItemResponse from(ServiceRequestQueryUseCase.ServiceRequestListItemResult result) {
            return new ServiceRequestListItemResponse(
                result.serviceRequestId(),
                result.passportId(),
                result.serialNumber(),
                result.modelName(),
                result.providerTenantId(),
                result.providerTenantName(),
                result.serviceType(),
                result.description(),
                result.status(),
                result.submittedAt()
            );
        }
    }

    public record PagedServiceRequestResponse(
        List<ServiceRequestListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
        static PagedServiceRequestResponse from(ServiceRequestQueryUseCase.PagedServiceRequestResult result) {
            return new PagedServiceRequestResponse(
                result.content().stream().map(ServiceRequestListItemResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
            );
        }
    }

    public record AcceptServiceRequestResponse(
        String serviceRequestId,
        String passportId,
        String status,
        Instant acceptedAt
    ) {
        static AcceptServiceRequestResponse from(AcceptServiceRequestResult result) {
            return new AcceptServiceRequestResponse(
                result.serviceRequestId(),
                result.passportId(),
                result.status(),
                result.acceptedAt()
            );
        }
    }

    public record RejectServiceRequestResponse(
        String serviceRequestId,
        String passportId,
        String status,
        Instant rejectedAt
    ) {
        static RejectServiceRequestResponse from(RejectServiceRequestResult result) {
            return new RejectServiceRequestResponse(
                result.serviceRequestId(),
                result.passportId(),
                result.status(),
                result.rejectedAt()
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
        static PresignEvidenceResponse from(PresignedEvidenceUploadResult result) {
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
        static CompleteEvidenceResponse from(EvidenceCompleteResult result) {
            return new CompleteEvidenceResponse(result.evidenceGroupId(), result.evidenceId(), result.status());
        }
    }
}
