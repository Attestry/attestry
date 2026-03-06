package io.attestry.workflow.application.claim;

import io.attestry.userauth.application.port.ObjectStoragePort;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.claim.command.CompleteClaimEvidenceCommand;
import io.attestry.workflow.application.claim.command.PresignClaimEvidenceCommand;
import io.attestry.workflow.application.port.ShipmentEvidencePort;
import io.attestry.workflow.application.shipment.result.PresignedShipmentEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.ShipmentEvidenceCompleteResult;
import io.attestry.workflow.application.support.EvidenceUploadSupport;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseClaimEvidenceService {

    private static final String OBJECT_KEY_PREFIX = "workflow/purchase-claim/";
    private static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

    private final ShipmentEvidencePort shipmentEvidencePort;
    private final ObjectStoragePort objectStoragePort;
    private final EvidenceUploadSupport evidenceUploadSupport;
    private final Clock clock;

    public PurchaseClaimEvidenceService(
        ShipmentEvidencePort shipmentEvidencePort,
        ObjectStoragePort objectStoragePort,
        EvidenceUploadSupport evidenceUploadSupport,
        Clock clock
    ) {
        this.shipmentEvidencePort = shipmentEvidencePort;
        this.objectStoragePort = objectStoragePort;
        this.evidenceUploadSupport = evidenceUploadSupport;
        this.clock = clock;
    }

    @Transactional
    public PresignedShipmentEvidenceUploadResult presignEvidence(
        AuthPrincipal principal,
        PresignClaimEvidenceCommand command
    ) {
        return evidenceUploadSupport.doPresign(
            shipmentEvidencePort, objectStoragePort,
            OBJECT_KEY_PREFIX, PRESIGN_TTL,
            command.tenantId(), principal.userId(),
            command.evidenceGroupId(), command.fileName(), command.contentType(),
            Instant.now(clock)
        );
    }

    @Transactional
    public ShipmentEvidenceCompleteResult completeEvidence(
        AuthPrincipal principal,
        CompleteClaimEvidenceCommand command
    ) {
        return evidenceUploadSupport.doComplete(
            shipmentEvidencePort, objectStoragePort,
            command.evidenceGroupId(), command.evidenceId(),
            command.sizeBytes(), command.fileHash(), Instant.now(clock)
        );
    }
}
