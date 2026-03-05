package io.attestry.workflow.application.claim;

import io.attestry.userauth.application.port.ObjectStoragePort;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.claim.command.CompleteClaimEvidenceCommand;
import io.attestry.workflow.application.claim.command.PresignClaimEvidenceCommand;
import io.attestry.workflow.application.port.ShipmentEvidencePort;
import io.attestry.workflow.application.shipment.result.PresignedShipmentEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.ShipmentEvidenceCompleteResult;
import io.attestry.workflow.application.support.EvidenceUploadSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
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
        String evidenceGroupId = command.evidenceGroupId() == null || command.evidenceGroupId().isBlank()
            ? UUID.randomUUID().toString()
            : command.evidenceGroupId().trim();

        Instant now = Instant.now(clock);
        shipmentEvidencePort.createEvidenceGroupIfAbsent(
            evidenceGroupId, command.tenantId(), command.groupId(), principal.userId(), now
        );

        String safeFileName = evidenceUploadSupport.normalizeFileName(command.fileName());
        String contentType = evidenceUploadSupport.normalizeContentType(command.contentType());
        String objectKey = evidenceUploadSupport.buildObjectKey(OBJECT_KEY_PREFIX, command.tenantId(), command.groupId(), evidenceGroupId, safeFileName);
        String evidenceId = UUID.randomUUID().toString();

        shipmentEvidencePort.createPendingEvidence(
            evidenceId, evidenceGroupId, objectKey, safeFileName, contentType, now
        );

        ObjectStoragePort.PresignedUpload presigned = objectStoragePort.issuePresignedUpload(objectKey, contentType, PRESIGN_TTL);
        return new PresignedShipmentEvidenceUploadResult(
            evidenceGroupId, evidenceId, objectKey, presigned.uploadUrl(), presigned.expiresAt()
        );
    }

    @Transactional
    public ShipmentEvidenceCompleteResult completeEvidence(
        AuthPrincipal principal,
        CompleteClaimEvidenceCommand command
    ) {
        ShipmentEvidencePort.ShipmentEvidenceView evidence = shipmentEvidencePort.findEvidenceById(
            command.evidenceGroupId(), command.evidenceId()
        ).orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.EVIDENCE_NOT_FOUND, "Evidence file not found"));

        evidenceUploadSupport.assertObjectUploaded(objectStoragePort.objectExists(evidence.objectKey()));
        evidenceUploadSupport.assertPositiveSize(command.sizeBytes());

        String normalizedHash = evidenceUploadSupport.normalizeHash(command.fileHash());
        shipmentEvidencePort.markEvidenceReady(
            command.evidenceGroupId(), command.evidenceId(),
            command.sizeBytes(), normalizedHash, Instant.now(clock)
        );

        return new ShipmentEvidenceCompleteResult(command.evidenceGroupId(), command.evidenceId(), "READY");
    }
}
