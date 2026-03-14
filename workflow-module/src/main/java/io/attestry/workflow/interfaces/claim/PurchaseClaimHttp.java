package io.attestry.workflow.interfaces.claim;

import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.claim.command.CompleteClaimEvidenceCommand;
import io.attestry.workflow.application.claim.command.PresignClaimEvidenceCommand;
import io.attestry.workflow.application.claim.command.SubmitPurchaseClaimCommand;
import io.attestry.workflow.application.claim.result.SubmitPurchaseClaimResult;
import io.attestry.workflow.application.shipment.result.EvidenceCompleteResult;
import io.attestry.workflow.application.shipment.result.PresignedEvidenceUploadResult;
import io.attestry.workflow.application.usecase.PurchaseClaimSubmitUseCase;
import io.attestry.workflow.interfaces.claim.dto.request.CompleteEvidenceRequest;
import io.attestry.workflow.interfaces.claim.dto.request.PresignEvidenceRequest;
import io.attestry.workflow.interfaces.claim.dto.request.SubmitClaimRequest;
import io.attestry.workflow.interfaces.claim.dto.response.ClaimEvidenceResponse;
import io.attestry.workflow.interfaces.claim.dto.response.CompleteEvidenceResponse;
import io.attestry.workflow.interfaces.claim.dto.response.MyClaimResponse;
import io.attestry.workflow.interfaces.claim.dto.response.PresignEvidenceResponse;
import io.attestry.workflow.interfaces.claim.dto.response.SubmitClaimResponse;
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
public class PurchaseClaimHttp {

    private final PurchaseClaimSubmitUseCase purchaseClaimSubmitUseCase;

    public PurchaseClaimHttp(PurchaseClaimSubmitUseCase purchaseClaimSubmitUseCase) {
        this.purchaseClaimSubmitUseCase = purchaseClaimSubmitUseCase;
    }

    @PostMapping("/evidence/presign")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PresignEvidenceResponse> presignEvidence(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody PresignEvidenceRequest request
    ) {
        PresignedEvidenceUploadResult result = purchaseClaimSubmitUseCase.presignEvidence(
            principal,
            new PresignClaimEvidenceCommand(
                request.evidenceGroupId(), request.fileName(), request.contentType()
            )
        );
        return ApiResponse.success(PresignEvidenceResponse.from(result));
    }

    @PostMapping("/evidence/complete")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CompleteEvidenceResponse> completeEvidence(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody CompleteEvidenceRequest request
    ) {
        EvidenceCompleteResult result = purchaseClaimSubmitUseCase.completeEvidence(
            principal,
            new CompleteClaimEvidenceCommand(
                request.evidenceGroupId(), request.evidenceId(),
                request.sizeBytes(), request.fileHash()
            )
        );
        return ApiResponse.success(CompleteEvidenceResponse.from(result));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SubmitClaimResponse> submit(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody SubmitClaimRequest request
    ) {
        SubmitPurchaseClaimResult result = purchaseClaimSubmitUseCase.submit(
            principal,
            new SubmitPurchaseClaimCommand(
                request.serialNumber(), request.modelName(),
                request.evidenceGroupId(), request.note()
            )
        );
        return ApiResponse.success(SubmitClaimResponse.from(result));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<MyClaimResponse>> listMyClaims(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success(purchaseClaimSubmitUseCase.listMyClaims(principal).stream()
            .map(MyClaimResponse::from)
            .toList());
    }

    @GetMapping("/{claimId}/evidences")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ClaimEvidenceResponse>> listMyClaimEvidences(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("claimId") String claimId
    ) {
        return ApiResponse.success(purchaseClaimSubmitUseCase.listMyClaimEvidences(principal, claimId).stream()
            .map(ClaimEvidenceResponse::from)
            .toList());
    }
}
