package io.attestry.workflow.interfaces.claim;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.claim.command.ApprovePurchaseClaimCommand;
import io.attestry.workflow.application.claim.result.ApprovePurchaseClaimResult;
import io.attestry.workflow.application.claim.result.ClaimEvidenceView;
import io.attestry.workflow.application.claim.result.PendingClaimView;
import io.attestry.workflow.application.claim.result.RejectPurchaseClaimResult;
import io.attestry.workflow.application.usecase.PurchaseClaimAdminUseCase;
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
@RequestMapping("/workflows/purchase-claims")
public class PurchaseClaimAdminHttp {
    private final PurchaseClaimAdminUseCase purchaseClaimAdminUseCase;

    public PurchaseClaimAdminHttp(PurchaseClaimAdminUseCase purchaseClaimAdminUseCase) {
        this.purchaseClaimAdminUseCase = purchaseClaimAdminUseCase;
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('SCOPE_PLATFORM_ADMIN')")
    public List<PendingClaimResponse> listAllPendingClaims(
        @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return purchaseClaimAdminUseCase.listPendingClaims(principal).stream()
            .map(PendingClaimResponse::from)
            .toList();
    }

    @GetMapping("/{claimId}/admin-evidences")
    @PreAuthorize("hasAuthority('SCOPE_PLATFORM_ADMIN')")
    public List<ClaimEvidenceResponse> listClaimEvidences(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("claimId") String claimId
    ) {
        return purchaseClaimAdminUseCase.listClaimEvidences(principal, claimId).stream()
            .map(ClaimEvidenceResponse::from)
            .toList();
    }

    @PostMapping("/{claimId}/approve")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('SCOPE_PLATFORM_ADMIN')")
    public ApproveClaimResponse approve(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("claimId") String claimId,
        @RequestBody ApproveClaimRequest request
    ) {
        ApprovePurchaseClaimResult result = purchaseClaimAdminUseCase.approve(
            principal, claimId,
            new ApprovePurchaseClaimCommand(request.manufacturedAt(), request.productionBatch(), request.factoryCode())
        );
        return ApproveClaimResponse.from(result);
    }

    @PostMapping("/{claimId}/reject")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('SCOPE_PLATFORM_ADMIN')")
    public RejectClaimResponse reject(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("claimId") String claimId,
        @RequestBody RejectClaimRequest request
    ) {
        RejectPurchaseClaimResult result = purchaseClaimAdminUseCase.reject(
            principal, claimId, request.reason()
        );
        return RejectClaimResponse.from(result);
    }

    public record PendingClaimResponse(
        String claimId, String claimantUserId,
        String serialNumber, String modelName,
        String evidenceGroupId, String note,
        String status, Instant submittedAt
    ) {
        static PendingClaimResponse from(PendingClaimView view) {
            return new PendingClaimResponse(
                view.claimId(), view.claimantUserId(),
                view.serialNumber(), view.modelName(),
                view.evidenceGroupId(), view.note(),
                view.status(), view.submittedAt()
            );
        }
    }

    public record ApproveClaimRequest(Instant manufacturedAt, String productionBatch, String factoryCode) {
    }

    public record RejectClaimRequest(String reason) {
    }

    public record ApproveClaimResponse(
        String claimId, String passportId, String assetId, String qrPublicCode, String status
    ) {
        static ApproveClaimResponse from(ApprovePurchaseClaimResult result) {
            return new ApproveClaimResponse(
                result.claimId(), result.passportId(), result.assetId(),
                result.qrPublicCode(), result.status()
            );
        }
    }

    public record RejectClaimResponse(String claimId, String status, String rejectionReason) {
        static RejectClaimResponse from(RejectPurchaseClaimResult result) {
            return new RejectClaimResponse(result.claimId(), result.status(), result.rejectionReason());
        }
    }

    public record ClaimEvidenceResponse(
        String evidenceId,
        String status,
        String downloadUrl,
        Instant expiresAt
    ) {
        static ClaimEvidenceResponse from(ClaimEvidenceView view) {
            return new ClaimEvidenceResponse(
                view.evidenceId(),
                view.status(),
                view.downloadUrl(),
                view.expiresAt()
            );
        }
    }
}
