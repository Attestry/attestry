package io.attestry.workflow.application.claim.internal;

import io.attestry.workflow.application.claim.result.SubmitPurchaseClaimResult;
import io.attestry.workflow.application.claim.view.ClaimEvidenceView;
import io.attestry.workflow.application.claim.view.MyClaimView;
import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PurchaseClaimViewFactory {

    public SubmitPurchaseClaimResult toSubmitResult(PurchaseClaim claim) {
        return new SubmitPurchaseClaimResult(
            claim.claimId(),
            claim.status().name(),
            claim.submittedAt()
        );
    }

    public MyClaimView toMyClaimView(PurchaseClaim claim, List<ClaimEvidenceView> evidences) {
        return new MyClaimView(
            claim.claimId(),
            claim.serialNumber(),
            claim.modelName(),
            claim.status().name(),
            claim.submittedAt(),
            claim.rejectionReason(),
            claim.passportId(),
            claim.assetId(),
            evidences
        );
    }
}
