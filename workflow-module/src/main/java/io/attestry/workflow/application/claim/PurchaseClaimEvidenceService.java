package io.attestry.workflow.application.claim;

import io.attestry.userauth.application.port.ObjectStoragePort;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.claim.command.CompleteClaimEvidenceCommand;
import io.attestry.workflow.application.claim.command.PresignClaimEvidenceCommand;
import io.attestry.workflow.application.port.ShipmentEvidencePort;
import io.attestry.workflow.application.shipment.result.PresignedShipmentEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.ShipmentEvidenceCompleteResult;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseClaimEvidenceService {

    private static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

    private final ShipmentEvidencePort shipmentEvidencePort;
    private final ObjectStoragePort objectStoragePort;
    private final Clock clock;

    public PurchaseClaimEvidenceService(
        ShipmentEvidencePort shipmentEvidencePort,
        ObjectStoragePort objectStoragePort,
        Clock clock
    ) {
        this.shipmentEvidencePort = shipmentEvidencePort;
        this.objectStoragePort = objectStoragePort;
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

        String safeFileName = normalizeFileName(command.fileName());
        String contentType = normalizeContentType(command.contentType());
        String objectKey = buildEvidenceObjectKey(command.tenantId(), command.groupId(), evidenceGroupId, safeFileName);
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

        if (!objectStoragePort.objectExists(evidence.objectKey())) {
            throw new WorkflowDomainException(WorkflowErrorCode.EVIDENCE_NOT_FOUND, "Uploaded object not found");
        }
        if (command.sizeBytes() <= 0) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "sizeBytes must be positive");
        }

        String normalizedHash = normalizeHash(command.fileHash());
        shipmentEvidencePort.markEvidenceReady(
            command.evidenceGroupId(), command.evidenceId(),
            command.sizeBytes(), normalizedHash, Instant.now(clock)
        );

        return new ShipmentEvidenceCompleteResult(command.evidenceGroupId(), command.evidenceId(), "READY");
    }

    private String normalizeHash(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "fileHash is required");
        }
        String normalized = hash.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[a-f0-9]{64}$")) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "fileHash must be sha256 hex");
        }
        return normalized;
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "fileName is required");
        }
        return fileName.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "contentType is required");
        }
        return contentType.trim();
    }

    private String buildEvidenceObjectKey(String tenantId, String groupId, String evidenceGroupId, String fileName) {
        return "workflow/purchase-claim/" + tenantId + "/" + groupId + "/" + evidenceGroupId + "/" + UUID.randomUUID() + "/" + fileName;
    }
}
