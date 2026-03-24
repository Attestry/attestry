package io.attestry.workflow.application.claim.usecase;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.claim.command.CompleteClaimEvidenceCommand;
import io.attestry.workflow.application.claim.command.PresignClaimEvidenceCommand;
import io.attestry.workflow.application.claim.command.SubmitPurchaseClaimCommand;
import io.attestry.workflow.application.claim.result.SubmitPurchaseClaimResult;
import io.attestry.workflow.application.claim.view.ClaimEvidenceView;
import io.attestry.workflow.application.claim.view.MyClaimView;
import io.attestry.workflow.application.shipment.result.PresignedEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.EvidenceCompleteResult;
import java.util.List;

public interface PurchaseClaimSubmitUseCase {

    SubmitPurchaseClaimResult submit(WorkflowActorContext principal, SubmitPurchaseClaimCommand command);

    List<MyClaimView> listMyClaims(WorkflowActorContext principal);

    List<ClaimEvidenceView> listMyClaimEvidences(WorkflowActorContext principal, String claimId);

    PresignedEvidenceUploadResult presignEvidence(WorkflowActorContext principal, PresignClaimEvidenceCommand command);

    EvidenceCompleteResult completeEvidence(WorkflowActorContext principal, CompleteClaimEvidenceCommand command);
}
