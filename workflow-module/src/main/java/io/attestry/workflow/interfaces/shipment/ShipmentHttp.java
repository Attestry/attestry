package io.attestry.workflow.interfaces.shipment;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.shipment.command.CompleteShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.PresignShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.ReleaseShipmentCommand;
import io.attestry.workflow.application.shipment.command.ReturnShipmentCommand;
import io.attestry.workflow.application.shipment.result.PresignedShipmentEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.ReleaseShipmentResult;
import io.attestry.workflow.application.shipment.result.ShipmentReleaseCandidateResult;
import io.attestry.workflow.application.shipment.result.ReturnShipmentResult;
import io.attestry.workflow.application.shipment.result.ShipmentDetailResult;
import io.attestry.workflow.application.shipment.result.ShipmentEvidenceCompleteResult;
import io.attestry.workflow.application.shipment.result.ShipmentViewResult;
import io.attestry.workflow.application.usecase.ShipmentEvidenceUseCase;
import io.attestry.workflow.application.usecase.ShipmentQueryUseCase;
import io.attestry.workflow.application.usecase.ShipmentReleaseUseCase;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflows")
public class ShipmentHttp {

    private final ShipmentEvidenceUseCase shipmentEvidenceUseCase;
    private final ShipmentReleaseUseCase shipmentReleaseUseCase;
    private final ShipmentQueryUseCase shipmentQueryUseCase;

    public ShipmentHttp(
            ShipmentEvidenceUseCase shipmentEvidenceUseCase,
            ShipmentReleaseUseCase shipmentReleaseUseCase,
            ShipmentQueryUseCase shipmentQueryUseCase) {
        this.shipmentEvidenceUseCase = shipmentEvidenceUseCase;
        this.shipmentReleaseUseCase = shipmentReleaseUseCase;
        this.shipmentQueryUseCase = shipmentQueryUseCase;
    }

    @PostMapping("/shipments/evidences/presign")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_BRAND_RELEASE')")
    public PresignedShipmentEvidenceUploadResponse presignEvidenceUpload(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody PresignShipmentEvidenceUploadRequest request) {
        PresignedShipmentEvidenceUploadResult result = shipmentEvidenceUseCase.presignEvidenceUpload(
                principal,
                new PresignShipmentEvidenceUploadCommand(
                        request.evidenceGroupId(),
                        request.fileName(),
                        request.contentType()));
        return PresignedShipmentEvidenceUploadResponse.from(result);
    }

    @PostMapping("/shipments/evidences/complete")
    @PreAuthorize("hasAuthority('SCOPE_BRAND_RELEASE')")
    public ShipmentEvidenceCompleteResponse completeEvidenceUpload(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody CompleteShipmentEvidenceUploadRequest request) {
        ShipmentEvidenceCompleteResult result = shipmentEvidenceUseCase.completeEvidenceUpload(
                principal,
                new CompleteShipmentEvidenceUploadCommand(
                        request.evidenceGroupId(),
                        request.evidenceId(),
                        request.sizeBytes(),
                        request.fileHash()));
        return ShipmentEvidenceCompleteResponse.from(result);
    }

    @PostMapping("/passports/{passportId}/shipments/release")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_BRAND_RELEASE')")
    public ReleaseShipmentResponse release(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("passportId") String passportId,
            @RequestBody ReleaseShipmentRequest request) {
        ReleaseShipmentResult result = shipmentReleaseUseCase.release(
                principal,
                passportId,
                new ReleaseShipmentCommand(request.evidenceGroupId()));
        return ReleaseShipmentResponse.from(result);
    }

    //TODO("pageing  적용 ")
    @GetMapping("/passports/{passportId}/shipments")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public List<ShipmentResponse> listShipments(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("passportId") String passportId) {
        return shipmentQueryUseCase.listByPassport(principal, passportId).stream()
                .map(ShipmentResponse::from)
                .toList();
    }

    //TODO("pageing  적용 ")
    @GetMapping("/shipments/release-candidates")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public List<ShipmentReleaseCandidateResponse> listReleaseCandidates(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return shipmentQueryUseCase.listReleaseCandidates(principal).stream()
                .map(ShipmentReleaseCandidateResponse::from)
                .toList();
    }

    //TODO("pageing  적용 ")
    @GetMapping("/shipments")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public List<ShipmentResponse> listAllShipments(@AuthenticationPrincipal AuthPrincipal principal) {
        return shipmentQueryUseCase.listByTenant(principal).stream()
                .map(ShipmentResponse::from)
                .toList();
    }

    @GetMapping("/shipments/{shipmentId}")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public ShipmentDetailResponse getShipmentDetail(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("shipmentId") String shipmentId) {
        return ShipmentDetailResponse.from(shipmentQueryUseCase.getShipmentDetail(principal, shipmentId));
    }

    @PostMapping("/shipments/{shipmentId}/return")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_BRAND_RELEASE')")
    public ReturnShipmentResponse returnShipment(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("shipmentId") String shipmentId,
            @RequestBody(required = false) ReturnShipmentRequest request) {
        ReturnShipmentResult result = shipmentReleaseUseCase.returnShipment(
                principal,
                shipmentId,
                new ReturnShipmentCommand(
                        request == null ? null : request.returnEvidenceGroupId(),
                        request == null ? null : request.reason()));
        return ReturnShipmentResponse.from(result);
    }

    public record PresignShipmentEvidenceUploadRequest(
            String evidenceGroupId,
            String fileName,
            String contentType) {
    }

    public record CompleteShipmentEvidenceUploadRequest(
            String evidenceGroupId,
            String evidenceId,
            long sizeBytes,
            String fileHash) {
    }

    public record ReleaseShipmentRequest(String evidenceGroupId) {
    }

    public record ReturnShipmentRequest(String returnEvidenceGroupId, String reason) {
    }

    public record PresignedShipmentEvidenceUploadResponse(
            String evidenceGroupId,
            String evidenceId,
            String objectKey,
            String uploadUrl,
            Instant expiresAt) {
        static PresignedShipmentEvidenceUploadResponse from(PresignedShipmentEvidenceUploadResult result) {
            return new PresignedShipmentEvidenceUploadResponse(
                    result.evidenceGroupId(),
                    result.evidenceId(),
                    result.objectKey(),
                    result.uploadUrl(),
                    result.expiresAt());
        }
    }

    public record ShipmentEvidenceCompleteResponse(
            String evidenceGroupId,
            String evidenceId,
            String status) {
        static ShipmentEvidenceCompleteResponse from(ShipmentEvidenceCompleteResult result) {
            return new ShipmentEvidenceCompleteResponse(result.evidenceGroupId(), result.evidenceId(), result.status());
        }
    }

    public record ReleaseShipmentResponse(
            String shipmentId,
            String tenantId,
            String passportId,
            int shipmentRound,
            String status,
            Instant releasedAt,
            String evidenceGroupId,
            String outboxEventId) {
        static ReleaseShipmentResponse from(ReleaseShipmentResult result) {
            return new ReleaseShipmentResponse(
                    result.shipmentId(),
                    result.tenantId(),
                    result.passportId(),
                    result.shipmentRound(),
                    result.status(),
                    result.releasedAt(),
                    result.evidenceGroupId(),
                    result.outboxEventId());
        }
    }

    public record ShipmentResponse(
            String shipmentId,
            String passportId,
            int shipmentRound,
            String status,
            Instant releasedAt,
            Instant returnedAt,
            Instant createdAt) {
        static ShipmentResponse from(ShipmentViewResult result) {
            return new ShipmentResponse(
                    result.shipmentId(),
                    result.passportId(),
                    result.shipmentRound(),
                    result.status(),
                    result.releasedAt(),
                    result.returnedAt(),
                    result.createdAt());
        }
    }

    public record ShipmentDetailResponse(
            String shipmentId,
            String tenantId,
            String passportId,
            int shipmentRound,
            String status,
            Instant releasedAt,
            String releasedByUserId,
            Instant returnedAt,
            String returnedByUserId,
            List<EvidenceFileResponse> releaseEvidenceFiles,
            List<EvidenceFileResponse> returnEvidenceFiles,
            Instant createdAt) {
        static ShipmentDetailResponse from(ShipmentDetailResult result) {
            return new ShipmentDetailResponse(
                    result.shipmentId(),
                    result.tenantId(),
                    result.passportId(),
                    result.shipmentRound(),
                    result.status(),
                    result.releasedAt(),
                    result.releasedByUserId(),
                    result.returnedAt(),
                    result.returnedByUserId(),
                    result.releaseEvidenceFiles().stream()
                            .map(EvidenceFileResponse::from)
                            .toList(),
                    result.returnEvidenceFiles().stream()
                            .map(EvidenceFileResponse::from)
                            .toList(),
                    result.createdAt());
        }
    }

    public record EvidenceFileResponse(
            String evidenceId,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String downloadUrl) {
        static EvidenceFileResponse from(ShipmentDetailResult.EvidenceFileResult result) {
            return new EvidenceFileResponse(
                    result.evidenceId(),
                    result.originalFileName(),
                    result.contentType(),
                    result.sizeBytes(),
                    result.downloadUrl());
        }
    }

    public record ShipmentReleaseCandidateResponse(
            String passportId,
            String assetId,
            String serialNumber,
            String modelId,
            String modelName,
            String productionBatch,
            String factoryCode) {
        static ShipmentReleaseCandidateResponse from(ShipmentReleaseCandidateResult result) {
            return new ShipmentReleaseCandidateResponse(
                    result.passportId(),
                    result.assetId(),
                    result.serialNumber(),
                    result.modelId(),
                    result.modelName(),
                    result.productionBatch(),
                    result.factoryCode());
        }
    }

    public record ReturnShipmentResponse(
            String shipmentId,
            String tenantId,
            String passportId,
            int shipmentRound,
            String status,
            Instant returnedAt,
            String returnedByUserId,
            String returnEvidenceGroupId,
            String outboxEventId) {
        static ReturnShipmentResponse from(ReturnShipmentResult result) {
            return new ReturnShipmentResponse(
                    result.shipmentId(),
                    result.tenantId(),
                    result.passportId(),
                    result.shipmentRound(),
                    result.status(),
                    result.returnedAt(),
                    result.returnedByUserId(),
                    result.returnEvidenceGroupId(),
                    result.outboxEventId());
        }
    }
}
