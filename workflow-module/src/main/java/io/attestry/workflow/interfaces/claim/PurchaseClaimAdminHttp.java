package io.attestry.workflow.interfaces.claim;

import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.claim.command.ApprovePurchaseClaimCommand;
import io.attestry.workflow.application.claim.result.ApprovePurchaseClaimResult;
import io.attestry.workflow.application.claim.result.RejectPurchaseClaimResult;
import io.attestry.workflow.application.usecase.PurchaseClaimAdminUseCase;
import io.attestry.workflow.interfaces.claim.dto.request.ApproveClaimRequest;
import io.attestry.workflow.interfaces.claim.dto.request.RejectClaimRequest;
import io.attestry.workflow.interfaces.claim.dto.response.ApproveClaimResponse;
import io.attestry.workflow.interfaces.claim.dto.response.ClaimEvidenceResponse;
import io.attestry.workflow.interfaces.claim.dto.response.PendingClaimResponse;
import io.attestry.workflow.interfaces.claim.dto.response.RejectClaimResponse;
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
    public ApiResponse<List<PendingClaimResponse>> listAllPendingClaims(
        @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ApiResponse.success(purchaseClaimAdminUseCase.listPendingClaims(principal).stream()
            .map(PendingClaimResponse::from)
            .toList());
    }

    @GetMapping("/{claimId}/admin-evidences")
    @PreAuthorize("hasAuthority('SCOPE_PLATFORM_ADMIN')")
    public ApiResponse<List<ClaimEvidenceResponse>> listClaimEvidences(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("claimId") String claimId
    ) {
        return ApiResponse.success(purchaseClaimAdminUseCase.listClaimEvidences(principal, claimId).stream()
            .map(ClaimEvidenceResponse::from)
            .toList());
    }

    @PostMapping("/{claimId}/approve")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('SCOPE_PLATFORM_ADMIN')")
    public ApiResponse<ApproveClaimResponse> approve(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("claimId") String claimId,
        @RequestBody ApproveClaimRequest request
    ) {
        ApprovePurchaseClaimResult result = purchaseClaimAdminUseCase.approve(
            principal, claimId,
            new ApprovePurchaseClaimCommand(request.manufacturedAt(), request.productionBatch(), request.factoryCode())
        );
        return ApiResponse.success(ApproveClaimResponse.from(result));
    }

    @PostMapping("/{claimId}/reject")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('SCOPE_PLATFORM_ADMIN')")
    public ApiResponse<RejectClaimResponse> reject(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("claimId") String claimId,
        @RequestBody RejectClaimRequest request
    ) {
        RejectPurchaseClaimResult result = purchaseClaimAdminUseCase.reject(
            principal, claimId, request.reason()
        );
        return ApiResponse.success(RejectClaimResponse.from(result));
    }
}
