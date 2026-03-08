package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.claim.command.CompleteClaimEvidenceCommand;
import io.attestry.workflow.application.claim.command.PresignClaimEvidenceCommand;
import io.attestry.workflow.application.claim.command.SubmitPurchaseClaimCommand;
import io.attestry.workflow.application.claim.result.ClaimEvidenceView;
import io.attestry.workflow.application.claim.result.MyClaimView;
import io.attestry.workflow.application.claim.result.SubmitPurchaseClaimResult;
import io.attestry.workflow.application.shipment.result.PresignedShipmentEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.ShipmentEvidenceCompleteResult;
import java.util.List;

public interface PurchaseClaimSubmitUseCase {

    SubmitPurchaseClaimResult submit(AuthPrincipal principal, SubmitPurchaseClaimCommand command);

    List<MyClaimView> listMyClaims(AuthPrincipal principal);

    List<ClaimEvidenceView> listMyClaimEvidences(AuthPrincipal principal, String claimId);

    PresignedShipmentEvidenceUploadResult presignEvidence(AuthPrincipal principal, PresignClaimEvidenceCommand command);

    ShipmentEvidenceCompleteResult completeEvidence(AuthPrincipal principal, CompleteClaimEvidenceCommand command);
}
