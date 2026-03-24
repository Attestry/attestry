package io.attestry.workflow.application.claim.usecase;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.claim.command.ApprovePurchaseClaimCommand;
import io.attestry.workflow.application.claim.result.ApprovePurchaseClaimResult;
import io.attestry.workflow.application.claim.result.RejectPurchaseClaimResult;
import io.attestry.workflow.application.claim.view.ClaimEvidenceView;
import io.attestry.workflow.application.claim.view.PendingClaimView;
import java.util.List;

public interface PurchaseClaimAdminUseCase {
    List<PendingClaimView> listPendingClaims(WorkflowActorContext principal);
    List<ClaimEvidenceView> listClaimEvidences(WorkflowActorContext principal, String claimId);

    ApprovePurchaseClaimResult approve(
        WorkflowActorContext principal,
        String claimId,
        ApprovePurchaseClaimCommand command
    );

    RejectPurchaseClaimResult reject(
        WorkflowActorContext principal,
        String claimId,
        String reason
    );
}
