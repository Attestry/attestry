package io.attestry.workflow.application.claim;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.claim.command.CompleteClaimEvidenceCommand;
import io.attestry.workflow.application.claim.command.PresignClaimEvidenceCommand;
import io.attestry.workflow.application.claim.command.SubmitPurchaseClaimCommand;
import io.attestry.workflow.application.claim.result.MyClaimView;
import io.attestry.workflow.application.claim.result.SubmitPurchaseClaimResult;
import io.attestry.workflow.application.port.ShipmentEvidencePort;
import io.attestry.workflow.application.shipment.result.PresignedShipmentEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.ShipmentEvidenceCompleteResult;
import io.attestry.workflow.application.usecase.PurchaseClaimSubmitUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import io.attestry.workflow.domain.claim.repository.PurchaseClaimRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseClaimSubmitService implements PurchaseClaimSubmitUseCase {

    private final PurchaseClaimRepository purchaseClaimRepository;
    private final ShipmentEvidencePort shipmentEvidencePort;
    private final PurchaseClaimEvidenceService evidenceService;
    private final Clock clock;

    public PurchaseClaimSubmitService(
        PurchaseClaimRepository purchaseClaimRepository,
        ShipmentEvidencePort shipmentEvidencePort,
        PurchaseClaimEvidenceService evidenceService,
        Clock clock
    ) {
        this.purchaseClaimRepository = purchaseClaimRepository;
        this.shipmentEvidencePort = shipmentEvidencePort;
        this.evidenceService = evidenceService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubmitPurchaseClaimResult submit(AuthPrincipal principal, SubmitPurchaseClaimCommand command) {
        if (principal.userId() == null || principal.userId().isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Authenticated user is required");
        }

        List<String> evidenceHashes = shipmentEvidencePort.findReadyEvidenceHashes(command.evidenceGroupId());
        if (evidenceHashes.isEmpty()) {
            throw new WorkflowDomainException(WorkflowErrorCode.CLAIM_EVIDENCE_INSUFFICIENT, "At least one READY evidence is required");
        }

        Instant now = Instant.now(clock);
        PurchaseClaim claim = PurchaseClaim.submit(
            UUID.randomUUID().toString(),
            command.tenantId(),
            command.groupId(),
            principal.userId(),
            command.serialNumber(),
            command.modelName(),
            command.evidenceGroupId(),
            command.note(),
            now
        );

        PurchaseClaim saved = purchaseClaimRepository.save(claim);
        return new SubmitPurchaseClaimResult(saved.claimId(), saved.status().name(), saved.submittedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyClaimView> listMyClaims(AuthPrincipal principal) {
        return purchaseClaimRepository.findByClaimantUserId(principal.userId()).stream()
            .map(c -> new MyClaimView(
                c.claimId(), c.tenantId(), c.groupId(),
                c.serialNumber(), c.modelName(),
                c.status().name(), c.submittedAt(),
                c.rejectionReason(), c.passportId(), c.assetId()
            ))
            .toList();
    }

    @Override
    @Transactional
    public PresignedShipmentEvidenceUploadResult presignEvidence(AuthPrincipal principal, PresignClaimEvidenceCommand command) {
        return evidenceService.presignEvidence(principal, command);
    }

    @Override
    @Transactional
    public ShipmentEvidenceCompleteResult completeEvidence(AuthPrincipal principal, CompleteClaimEvidenceCommand command) {
        return evidenceService.completeEvidence(principal, command);
    }
}
