package io.attestry.workflow.interfaces.shipment;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.shipment.command.CompleteShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.PresignShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.ReleaseShipmentCommand;
import io.attestry.workflow.application.shipment.command.ReturnShipmentCommand;
import io.attestry.workflow.application.shipment.result.PresignedShipmentEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.ReleaseShipmentResult;
import io.attestry.workflow.application.shipment.result.ReturnShipmentResult;
import io.attestry.workflow.application.shipment.result.ShipmentEvidenceCompleteResult;
import io.attestry.workflow.application.shipment.result.ShipmentEvidenceViewResult;
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
        ShipmentQueryUseCase shipmentQueryUseCase
    ) {
        this.shipmentEvidenceUseCase = shipmentEvidenceUseCase;
        this.shipmentReleaseUseCase = shipmentReleaseUseCase;
        this.shipmentQueryUseCase = shipmentQueryUseCase;
    }

    @PostMapping("/tenants/{tenantId}/groups/{groupId}/shipments/evidences/presign")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_BRAND_RELEASE')")
    public PresignedShipmentEvidenceUploadResponse presignEvidenceUpload(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("groupId") String groupId,
        @RequestBody PresignShipmentEvidenceUploadRequest request
    ) {
        PresignedShipmentEvidenceUploadResult result = shipmentEvidenceUseCase.presignEvidenceUpload(
            principal,
            tenantId,
            groupId,
            new PresignShipmentEvidenceUploadCommand(
                request.evidenceGroupId(),
                request.fileName(),
                request.contentType()
            )
        );
        return PresignedShipmentEvidenceUploadResponse.from(result);
    }

    @PostMapping("/tenants/{tenantId}/groups/{groupId}/shipments/evidences/complete")
    @PreAuthorize("hasAuthority('SCOPE_BRAND_RELEASE')")
    public ShipmentEvidenceCompleteResponse completeEvidenceUpload(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("groupId") String groupId,
        @RequestBody CompleteShipmentEvidenceUploadRequest request
    ) {
        ShipmentEvidenceCompleteResult result = shipmentEvidenceUseCase.completeEvidenceUpload(
            principal,
            tenantId,
            groupId,
            new CompleteShipmentEvidenceUploadCommand(
                request.evidenceGroupId(),
                request.evidenceId(),
                request.sizeBytes(),
                request.fileHash()
            )
        );
        return ShipmentEvidenceCompleteResponse.from(result);
    }

    @PostMapping("/tenants/{tenantId}/groups/{groupId}/passports/{passportId}/shipments/release")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_BRAND_RELEASE')")
    public ReleaseShipmentResponse release(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("groupId") String groupId,
        @PathVariable("passportId") String passportId,
        @RequestBody ReleaseShipmentRequest request
    ) {
        ReleaseShipmentResult result = shipmentReleaseUseCase.release(
            principal,
            tenantId,
            groupId,
            passportId,
            new ReleaseShipmentCommand(request.evidenceGroupId())
        );
        return ReleaseShipmentResponse.from(result);
    }

    @GetMapping("/tenants/{tenantId}/groups/{groupId}/passports/{passportId}/shipments")
    @PreAuthorize("hasAuthority('SCOPE_BRAND_RELEASE')")
    public List<ShipmentResponse> listShipments(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("groupId") String groupId,
        @PathVariable("passportId") String passportId
    ) {
        return shipmentQueryUseCase.listByPassport(principal, tenantId, groupId, passportId).stream()
            .map(ShipmentResponse::from)
            .toList();
    }

    @GetMapping("/tenants/{tenantId}/groups/{groupId}/shipments/{shipmentId}/evidences")
    @PreAuthorize("hasAuthority('SCOPE_BRAND_RELEASE')")
    public List<ShipmentEvidenceResponse> listShipmentEvidences(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("groupId") String groupId,
        @PathVariable("shipmentId") String shipmentId
    ) {
        return shipmentQueryUseCase.listEvidenceByShipmentId(principal, tenantId, groupId, shipmentId).stream()
            .map(ShipmentEvidenceResponse::from)
            .toList();
    }

    @PostMapping("/tenants/{tenantId}/groups/{groupId}/shipments/{shipmentId}/return")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_BRAND_RELEASE')")
    public ReturnShipmentResponse returnShipment(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("groupId") String groupId,
        @PathVariable("shipmentId") String shipmentId,
        @RequestBody(required = false) ReturnShipmentRequest request
    ) {
        ReturnShipmentResult result = shipmentReleaseUseCase.returnShipment(
            principal,
            tenantId,
            groupId,
            shipmentId,
            new ReturnShipmentCommand(
                request == null ? null : request.returnEvidenceGroupId(),
                request == null ? null : request.reason()
            )
        );
        return ReturnShipmentResponse.from(result);
    }

    public record PresignShipmentEvidenceUploadRequest(
        String evidenceGroupId,
        String fileName,
        String contentType
    ) {
    }

    public record CompleteShipmentEvidenceUploadRequest(
        String evidenceGroupId,
        String evidenceId,
        long sizeBytes,
        String fileHash
    ) {
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
        Instant expiresAt
    ) {
        static PresignedShipmentEvidenceUploadResponse from(PresignedShipmentEvidenceUploadResult result) {
            return new PresignedShipmentEvidenceUploadResponse(
                result.evidenceGroupId(),
                result.evidenceId(),
                result.objectKey(),
                result.uploadUrl(),
                result.expiresAt()
            );
        }
    }

    public record ShipmentEvidenceCompleteResponse(
        String evidenceGroupId,
        String evidenceId,
        String status
    ) {
        static ShipmentEvidenceCompleteResponse from(ShipmentEvidenceCompleteResult result) {
            return new ShipmentEvidenceCompleteResponse(result.evidenceGroupId(), result.evidenceId(), result.status());
        }
    }

    public record ReleaseShipmentResponse(
        String shipmentId,
        String tenantId,
        String groupId,
        String passportId,
        int shipmentRound,
        String status,
        Instant releasedAt,
        String evidenceGroupId,
        String outboxEventId
    ) {
        static ReleaseShipmentResponse from(ReleaseShipmentResult result) {
            return new ReleaseShipmentResponse(
                result.shipmentId(),
                result.tenantId(),
                result.groupId(),
                result.passportId(),
                result.shipmentRound(),
                result.status(),
                result.releasedAt(),
                result.evidenceGroupId(),
                result.outboxEventId()
            );
        }
    }

    public record ShipmentResponse(
        String shipmentId,
        String tenantId,
        String groupId,
        String passportId,
        int shipmentRound,
        String status,
        Instant releasedAt,
        String releasedByUserId,
        String releasedByGroupId,
        String evidenceGroupId,
        Instant returnedAt,
        String returnedByUserId,
        String returnEvidenceGroupId,
        Instant createdAt
    ) {
        static ShipmentResponse from(ShipmentViewResult result) {
            return new ShipmentResponse(
                result.shipmentId(),
                result.tenantId(),
                result.groupId(),
                result.passportId(),
                result.shipmentRound(),
                result.status(),
                result.releasedAt(),
                result.releasedByUserId(),
                result.releasedByGroupId(),
                result.evidenceGroupId(),
                result.returnedAt(),
                result.returnedByUserId(),
                result.returnEvidenceGroupId(),
                result.createdAt()
            );
        }
    }

    public record ShipmentEvidenceResponse(String evidenceId, String evidenceGroupId, String fileHash) {
        static ShipmentEvidenceResponse from(ShipmentEvidenceViewResult result) {
            return new ShipmentEvidenceResponse(result.evidenceId(), result.evidenceGroupId(), result.fileHash());
        }
    }

    public record ReturnShipmentResponse(
        String shipmentId,
        String tenantId,
        String groupId,
        String passportId,
        int shipmentRound,
        String status,
        Instant returnedAt,
        String returnedByUserId,
        String returnEvidenceGroupId,
        String outboxEventId
    ) {
        static ReturnShipmentResponse from(ReturnShipmentResult result) {
            return new ReturnShipmentResponse(
                result.shipmentId(),
                result.tenantId(),
                result.groupId(),
                result.passportId(),
                result.shipmentRound(),
                result.status(),
                result.returnedAt(),
                result.returnedByUserId(),
                result.returnEvidenceGroupId(),
                result.outboxEventId()
            );
        }
    }
}
