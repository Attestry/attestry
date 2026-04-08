package io.attestry.workflow.interfaces.shipment;

import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.shipment.command.CompleteShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.PresignShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.ReleaseShipmentCommand;
import io.attestry.workflow.application.shipment.command.ReturnShipmentCommand;
import io.attestry.workflow.application.shipment.result.EvidenceCompleteResult;
import io.attestry.workflow.application.shipment.result.PresignedEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.ReleaseShipmentResult;
import io.attestry.workflow.application.shipment.result.ReturnShipmentResult;
import io.attestry.workflow.application.shipment.command.ShipmentEvidenceUseCase;
import io.attestry.workflow.application.shipment.command.ShipmentReleaseUseCase;
import io.attestry.workflow.application.shipment.query.ShipmentQueryUseCase;
import io.attestry.workflow.application.shipment.view.PagedReleaseCandidateView;
import io.attestry.workflow.application.shipment.view.PagedShipmentView;
import io.attestry.workflow.interfaces.shipment.dto.request.CompleteShipmentEvidenceUploadRequest;
import io.attestry.workflow.interfaces.shipment.dto.request.PresignShipmentEvidenceUploadRequest;
import io.attestry.workflow.interfaces.shipment.dto.request.ReleaseShipmentRequest;
import io.attestry.workflow.interfaces.shipment.dto.request.ReturnShipmentRequest;
import io.attestry.workflow.interfaces.shipment.dto.response.PagedShipmentReleaseCandidateResponse;
import io.attestry.workflow.interfaces.shipment.dto.response.PagedShipmentResponse;
import io.attestry.workflow.interfaces.shipment.dto.response.PresignedShipmentEvidenceUploadResponse;
import io.attestry.workflow.interfaces.shipment.dto.response.ReleaseShipmentResponse;
import io.attestry.workflow.interfaces.shipment.dto.response.ReturnShipmentResponse;
import io.attestry.workflow.interfaces.shipment.dto.response.ShipmentDetailResponse;
import io.attestry.workflow.interfaces.shipment.dto.response.ShipmentEvidenceCompleteResponse;
import io.attestry.workflow.interfaces.shipment.dto.response.ShipmentReleaseCandidateResponse;
import io.attestry.workflow.interfaces.shipment.dto.response.ShipmentResponse;
import java.util.List;

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
public class ShipmentHttp {

    private final ShipmentEvidenceUseCase shipmentEvidenceUseCase;
    private final ShipmentReleaseUseCase shipmentReleaseUseCase;
    private final ShipmentQueryUseCase shipmentQueryUseCase;


    @PostMapping("/shipments/evidences/presign")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_BRAND_RELEASE')")
    public ApiResponse<PresignedShipmentEvidenceUploadResponse> presignEvidenceUpload(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody PresignShipmentEvidenceUploadRequest request) {
        PresignedEvidenceUploadResult result = shipmentEvidenceUseCase.presignEvidenceUpload(
                actor(principal),
                new PresignShipmentEvidenceUploadCommand(
                        request.evidenceGroupId(),
                        request.fileName(),
                        request.contentType()));
        return ApiResponse.success(PresignedShipmentEvidenceUploadResponse.from(result));
    }

    @PostMapping("/shipments/evidences/complete")
    @PreAuthorize("hasAuthority('SCOPE_BRAND_RELEASE')")
    public ApiResponse<ShipmentEvidenceCompleteResponse> completeEvidenceUpload(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody CompleteShipmentEvidenceUploadRequest request) {
        EvidenceCompleteResult result = shipmentEvidenceUseCase.completeEvidenceUpload(
                actor(principal),
                new CompleteShipmentEvidenceUploadCommand(
                        request.evidenceGroupId(),
                        request.evidenceId(),
                        request.sizeBytes(),
                        request.fileHash()));
        return ApiResponse.success(ShipmentEvidenceCompleteResponse.from(result));
    }

    @PostMapping("/passports/{passportId}/shipments/release")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_BRAND_RELEASE')")
    public ApiResponse<ReleaseShipmentResponse> release(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("passportId") String passportId,
            @RequestBody ReleaseShipmentRequest request) {
        ReleaseShipmentResult result = shipmentReleaseUseCase.release(
                actor(principal),
                passportId,
                new ReleaseShipmentCommand(request.evidenceGroupId()));
        return ApiResponse.success(ReleaseShipmentResponse.from(result));
    }


    @GetMapping("/shipments/release-candidates")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public ApiResponse<PagedShipmentReleaseCandidateResponse> listReleaseCandidates(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "keyword", required = false) String keyword) {
        PagedReleaseCandidateView result =
                shipmentQueryUseCase.listReleaseCandidates(actor(principal), page, size, keyword);
        List<ShipmentReleaseCandidateResponse> content = result.content().stream()
                .map(ShipmentReleaseCandidateResponse::from)
                .toList();
        return ApiResponse.success(new PagedShipmentReleaseCandidateResponse(
                content, result.page(), result.size(),
                result.totalElements(), result.totalPages()));
    }

    @GetMapping("/shipments")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public ApiResponse<PagedShipmentResponse> listAllShipments(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "keyword", required = false) String keyword) {
        PagedShipmentView result =
                shipmentQueryUseCase.listByTenant(actor(principal), page, size, keyword);
        List<ShipmentResponse> content = result.content().stream()
                .map(ShipmentResponse::from)
                .toList();
        return ApiResponse.success(new PagedShipmentResponse(
                content, result.page(), result.size(),
                result.totalElements(), result.totalPages()));
    }

    @GetMapping("/shipments/{shipmentId}")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public ApiResponse<ShipmentDetailResponse> getShipmentDetail(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("shipmentId") String shipmentId) {
        return ApiResponse.success(ShipmentDetailResponse.from(shipmentQueryUseCase.getShipmentDetail(actor(principal), shipmentId)));
    }

    @PostMapping("/shipments/{shipmentId}/return")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_BRAND_RELEASE')")
    public ApiResponse<ReturnShipmentResponse> returnShipment(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("shipmentId") String shipmentId,
            @RequestBody(required = false) ReturnShipmentRequest request) {
        ReturnShipmentResult result = shipmentReleaseUseCase.returnShipment(
                actor(principal),
                shipmentId,
                new ReturnShipmentCommand(
                        request == null ? null : request.returnEvidenceGroupId(),
                        request == null ? null : request.reason()));
        return ApiResponse.success(ReturnShipmentResponse.from(result));
    }

    private WorkflowActorContext actor(AuthPrincipal principal) {
        return WorkflowActorContext.from(principal);
    }
}
