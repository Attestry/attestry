package io.attestry.workflow.interfaces.servicerequest;

import io.attestry.commonlib.infrastructure.ApiResponse;
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
import io.attestry.workflow.interfaces.servicerequest.dto.request.AcceptServiceRequestRequest;
import io.attestry.workflow.interfaces.servicerequest.dto.request.CancelServiceRequestRequest;
import io.attestry.workflow.interfaces.servicerequest.dto.request.CompleteEvidenceRequest;
import io.attestry.workflow.interfaces.servicerequest.dto.request.CompleteServiceRequestRequest;
import io.attestry.workflow.interfaces.servicerequest.dto.request.GrantServiceConsentRequest;
import io.attestry.workflow.interfaces.servicerequest.dto.request.PresignEvidenceRequest;
import io.attestry.workflow.interfaces.servicerequest.dto.request.RejectServiceRequestRequest;
import io.attestry.workflow.interfaces.servicerequest.dto.request.RevokeServiceConsentRequest;
import io.attestry.workflow.interfaces.servicerequest.dto.request.SubmitServiceRequestRequest;
import io.attestry.workflow.interfaces.servicerequest.dto.response.AcceptServiceRequestResponse;
import io.attestry.workflow.interfaces.servicerequest.dto.response.CancelServiceRequestResponse;
import io.attestry.workflow.interfaces.servicerequest.dto.response.CompleteEvidenceResponse;
import io.attestry.workflow.interfaces.servicerequest.dto.response.CompleteServiceRequestResponse;
import io.attestry.workflow.interfaces.servicerequest.dto.response.GrantServiceConsentResponse;
import io.attestry.workflow.interfaces.servicerequest.dto.response.PagedServiceRequestResponse;
import io.attestry.workflow.interfaces.servicerequest.dto.response.PresignEvidenceResponse;
import io.attestry.workflow.interfaces.servicerequest.dto.response.RejectServiceRequestResponse;
import io.attestry.workflow.interfaces.servicerequest.dto.response.RevokeServiceConsentResponse;
import io.attestry.workflow.interfaces.servicerequest.dto.response.SubmitServiceRequestResponse;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
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

    @PostMapping("/passports/{passportId}/service-consent")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_OWNER_SERVICE_CREATE')")
    public ApiResponse<GrantServiceConsentResponse> submit(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("passportId") String passportId,
        @Valid @RequestBody GrantServiceConsentRequest request
    ) {
        GrantServiceConsentResult result = serviceConsentUseCase.submit(
            principal,
            passportId,
            new GrantServiceConsentCommand(
                request.providerTenantId(),
                request.beforeEvidenceGroupId(),
                request.serviceRequestMethod(),
                request.symptomDescription(),
                request.requestedReservationAt(),
                request.contactMemo()
            )
        );
        return ApiResponse.success(GrantServiceConsentResponse.from(result));
    }

    @PostMapping("/passports/{passportId}/service-consent/revoke")
    @PreAuthorize("hasAuthority('SCOPE_OWNER_SERVICE_CREATE')")
    public ApiResponse<RevokeServiceConsentResponse> revokeConsent(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("passportId") String passportId,
        @RequestBody RevokeServiceConsentRequest request
    ) {
        RevokeServiceConsentResult result = serviceConsentUseCase.revokeConsent(
            principal,
            passportId,
            request.providerTenantId()
        );
        return ApiResponse.success(RevokeServiceConsentResponse.from(result));
    }

    // --- Submit endpoint (OWNER) ---

    @PostMapping("/service-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_OWNER_SERVICE_CREATE')")
    public ApiResponse<SubmitServiceRequestResponse> submitRequest(
        @AuthenticationPrincipal AuthPrincipal principal,
        @Valid @RequestBody SubmitServiceRequestRequest request
    ) {
        SubmitServiceRequestResult result = serviceSubmitUseCase.submit(
            principal,
            new SubmitServiceRequestCommand(
                request.passportId(),
                request.providerTenantId(),
                request.beforeEvidenceGroupId(),
                request.serviceRequestMethod(),
                request.symptomDescription(),
                request.requestedReservationAt(),
                request.contactMemo()
            )
        );
        return ApiResponse.success(SubmitServiceRequestResponse.from(result));
    }

    @GetMapping("/service-requests/me")
    @PreAuthorize("hasAuthority('SCOPE_OWNER_SERVICE_CREATE')")
    public ApiResponse<PagedServiceRequestResponse> listMyRequests(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        ServiceRequestQueryUseCase.PagedServiceRequestResult result = serviceRequestQueryUseCase
            .listMyRequests(principal, status, page, size);
        return ApiResponse.success(PagedServiceRequestResponse.from(result));
    }

    @GetMapping("/tenants/{tenantId}/service-requests")
    @PreAuthorize("hasAnyAuthority('SCOPE_SERVICE_COMPLETE', 'SCOPE_TENANT_READ_ONLY')")
    public ApiResponse<PagedServiceRequestResponse> listProviderRequests(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        ServiceRequestQueryUseCase.PagedServiceRequestResult result = serviceRequestQueryUseCase
            .listProviderRequests(principal, tenantId, status, page, size);
        return ApiResponse.success(PagedServiceRequestResponse.from(result));
    }

    @PostMapping("/tenants/{tenantId}/service-requests/{serviceRequestId}/accept")
    @PreAuthorize("hasAuthority('SCOPE_SERVICE_COMPLETE')")
    public ApiResponse<AcceptServiceRequestResponse> accept(
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
        return ApiResponse.success(AcceptServiceRequestResponse.from(result));
    }

    @PostMapping("/tenants/{tenantId}/service-requests/{serviceRequestId}/reject")
    @PreAuthorize("hasAuthority('SCOPE_SERVICE_COMPLETE')")
    public ApiResponse<RejectServiceRequestResponse> reject(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("serviceRequestId") String serviceRequestId,
        @Valid @RequestBody(required = false) RejectServiceRequestRequest request
    ) {
        RejectServiceRequestResult result = serviceRejectUseCase.reject(
            principal,
            tenantId,
            serviceRequestId,
            new RejectServiceRequestCommand(request == null ? null : request.reason())
        );
        return ApiResponse.success(RejectServiceRequestResponse.from(result));
    }

    @PostMapping("/tenants/{tenantId}/service-requests/{serviceRequestId}/complete")
    @PreAuthorize("hasAuthority('SCOPE_SERVICE_COMPLETE')")
    public ApiResponse<CompleteServiceRequestResponse> complete(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("serviceRequestId") String serviceRequestId,
        @Valid @RequestBody CompleteServiceRequestRequest request
    ) {
        CompleteServiceRequestResult result = serviceCompleteUseCase.complete(
            principal,
            tenantId,
            serviceRequestId,
            new CompleteServiceRequestCommand(
                request.serviceType(),
                request.afterEvidenceGroupId(),
                request.serviceResult(),
                request.completionMemo()
            )
        );
        return ApiResponse.success(CompleteServiceRequestResponse.from(result));
    }

    @PostMapping("/service-requests/{serviceRequestId}/cancel")
    @PreAuthorize("hasAuthority('SCOPE_OWNER_SERVICE_CREATE')")
    public ApiResponse<CancelServiceRequestResponse> cancel(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("serviceRequestId") String serviceRequestId,
        @Valid @RequestBody(required = false) CancelServiceRequestRequest request
    ) {
        CancelServiceRequestResult result = serviceCancelUseCase.cancel(
            principal,
            serviceRequestId,
            request == null ? null : request.cancelReason()
        );
        return ApiResponse.success(CancelServiceRequestResponse.from(result));
    }

    @PostMapping("/service-requests/evidences/presign")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_OWNER_SERVICE_CREATE')")
    public ApiResponse<PresignEvidenceResponse> presignOwnerEvidence(
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
        return ApiResponse.success(PresignEvidenceResponse.from(result));
    }

    @PostMapping("/service-requests/evidences/complete")
    @PreAuthorize("hasAuthority('SCOPE_OWNER_SERVICE_CREATE')")
    public ApiResponse<CompleteEvidenceResponse> completeOwnerEvidence(
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
        return ApiResponse.success(CompleteEvidenceResponse.from(result));
    }

    @PostMapping("/tenants/{tenantId}/service-requests/evidences/presign")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_SERVICE_COMPLETE')")
    public ApiResponse<PresignEvidenceResponse> presignProviderEvidence(
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
        return ApiResponse.success(PresignEvidenceResponse.from(result));
    }

    @PostMapping("/tenants/{tenantId}/service-requests/evidences/complete")
    @PreAuthorize("hasAuthority('SCOPE_SERVICE_COMPLETE')")
    public ApiResponse<CompleteEvidenceResponse> completeProviderEvidence(
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
        return ApiResponse.success(CompleteEvidenceResponse.from(result));
    }
}
