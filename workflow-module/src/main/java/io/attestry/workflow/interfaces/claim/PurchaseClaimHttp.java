package io.attestry.workflow.interfaces.claim;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.claim.command.CompleteClaimEvidenceCommand;
import io.attestry.workflow.application.claim.command.PresignClaimEvidenceCommand;
import io.attestry.workflow.application.claim.command.SubmitPurchaseClaimCommand;
import io.attestry.workflow.application.claim.result.MyClaimView;
import io.attestry.workflow.application.claim.result.SubmitPurchaseClaimResult;
import io.attestry.workflow.application.shipment.result.PresignedShipmentEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.ShipmentEvidenceCompleteResult;
import io.attestry.workflow.application.usecase.PurchaseClaimSubmitUseCase;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflows/purchase-claims")
public class PurchaseClaimHttp {

    private final PurchaseClaimSubmitUseCase purchaseClaimSubmitUseCase;

    public PurchaseClaimHttp(PurchaseClaimSubmitUseCase purchaseClaimSubmitUseCase) {
        this.purchaseClaimSubmitUseCase = purchaseClaimSubmitUseCase;
    }

    @PostMapping("/evidence/presign")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public PresignEvidenceResponse presignEvidence(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody PresignEvidenceRequest request
    ) {
        PresignedShipmentEvidenceUploadResult result = purchaseClaimSubmitUseCase.presignEvidence(
            principal,
            new PresignClaimEvidenceCommand(
                request.tenantId(), request.groupId(),
                request.evidenceGroupId(), request.fileName(), request.contentType()
            )
        );
        return PresignEvidenceResponse.from(result);
    }

    @PostMapping("/evidence/complete")
    @PreAuthorize("isAuthenticated()")
    public CompleteEvidenceResponse completeEvidence(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody CompleteEvidenceRequest request
    ) {
        ShipmentEvidenceCompleteResult result = purchaseClaimSubmitUseCase.completeEvidence(
            principal,
            new CompleteClaimEvidenceCommand(
                request.tenantId(), request.groupId(),
                request.evidenceGroupId(), request.evidenceId(),
                request.sizeBytes(), request.fileHash()
            )
        );
        return CompleteEvidenceResponse.from(result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public SubmitClaimResponse submit(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody SubmitClaimRequest request
    ) {
        SubmitPurchaseClaimResult result = purchaseClaimSubmitUseCase.submit(
            principal,
            new SubmitPurchaseClaimCommand(
                request.tenantId(), request.groupId(),
                request.serialNumber(), request.modelName(),
                request.evidenceGroupId(), request.note()
            )
        );
        return SubmitClaimResponse.from(result);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public List<MyClaimResponse> listMyClaims(@AuthenticationPrincipal AuthPrincipal principal) {
        return purchaseClaimSubmitUseCase.listMyClaims(principal).stream()
            .map(MyClaimResponse::from)
            .toList();
    }

    public record PresignEvidenceRequest(
        String tenantId, String groupId,
        String evidenceGroupId, String fileName, String contentType
    ) {
    }

    public record CompleteEvidenceRequest(
        String tenantId, String groupId,
        String evidenceGroupId, String evidenceId,
        long sizeBytes, String fileHash
    ) {
    }

    public record SubmitClaimRequest(
        String tenantId, String groupId,
        String serialNumber, String modelName,
        String evidenceGroupId, String note
    ) {
    }

    public record PresignEvidenceResponse(
        String evidenceGroupId, String evidenceId,
        String objectKey, String uploadUrl, Instant expiresAt
    ) {
        static PresignEvidenceResponse from(PresignedShipmentEvidenceUploadResult result) {
            return new PresignEvidenceResponse(
                result.evidenceGroupId(), result.evidenceId(),
                result.objectKey(), result.uploadUrl(), result.expiresAt()
            );
        }
    }

    public record CompleteEvidenceResponse(String evidenceGroupId, String evidenceId, String status) {
        static CompleteEvidenceResponse from(ShipmentEvidenceCompleteResult result) {
            return new CompleteEvidenceResponse(result.evidenceGroupId(), result.evidenceId(), result.status());
        }
    }

    public record SubmitClaimResponse(String claimId, String status, Instant submittedAt) {
        static SubmitClaimResponse from(SubmitPurchaseClaimResult result) {
            return new SubmitClaimResponse(result.claimId(), result.status(), result.submittedAt());
        }
    }

    public record MyClaimResponse(
        String claimId, String tenantId, String groupId,
        String serialNumber, String modelName,
        String status, Instant submittedAt,
        String rejectionReason, String passportId, String assetId
    ) {
        static MyClaimResponse from(MyClaimView view) {
            return new MyClaimResponse(
                view.claimId(), view.tenantId(), view.groupId(),
                view.serialNumber(), view.modelName(),
                view.status(), view.submittedAt(),
                view.rejectionReason(), view.passportId(), view.assetId()
            );
        }
    }
}
