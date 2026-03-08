package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.claim.command.ApprovePurchaseClaimCommand;
import io.attestry.workflow.application.claim.result.ApprovePurchaseClaimResult;
import io.attestry.workflow.application.claim.result.ClaimEvidenceView;
import io.attestry.workflow.application.claim.result.PendingClaimView;
import io.attestry.workflow.application.claim.result.RejectPurchaseClaimResult;
import java.util.List;

public interface PurchaseClaimAdminUseCase {
    List<PendingClaimView> listPendingClaims(AuthPrincipal principal);
    List<ClaimEvidenceView> listClaimEvidences(AuthPrincipal principal, String claimId);

    ApprovePurchaseClaimResult approve(
        AuthPrincipal principal,
        String claimId,
        ApprovePurchaseClaimCommand command
    );

    RejectPurchaseClaimResult reject(
        AuthPrincipal principal,
        String claimId,
        String reason
    );
}
