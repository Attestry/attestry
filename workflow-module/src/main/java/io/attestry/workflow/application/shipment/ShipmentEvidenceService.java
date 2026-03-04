package io.attestry.workflow.application.shipment;

import io.attestry.userauth.application.port.ObjectStoragePort;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.ShipmentEvidencePort;
import io.attestry.workflow.application.shipment.command.CompleteShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.PresignShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.result.PresignedShipmentEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.ShipmentEvidenceCompleteResult;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.ShipmentEvidenceUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentEvidenceService implements ShipmentEvidenceUseCase {

    private static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

    private final ShipmentEvidencePort shipmentEvidencePort;
    private final ObjectStoragePort objectStoragePort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final Clock clock;

    public ShipmentEvidenceService(
        ShipmentEvidencePort shipmentEvidencePort,
        ObjectStoragePort objectStoragePort,
        WorkflowAuthorizationSupport authorizationSupport,
        Clock clock
    ) {
        this.shipmentEvidencePort = shipmentEvidencePort;
        this.objectStoragePort = objectStoragePort;
        this.authorizationSupport = authorizationSupport;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PresignedShipmentEvidenceUploadResult presignEvidenceUpload(
        AuthPrincipal principal,
        String tenantId,
        String groupId,
        PresignShipmentEvidenceUploadCommand command
    ) {
        authorizationSupport.assertTenantAndGroupContext(principal, tenantId, groupId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.BRAND_RELEASE, "shipment:evidence:presign");

        String evidenceGroupId = command.evidenceGroupId() == null || command.evidenceGroupId().isBlank()
            ? UUID.randomUUID().toString()
            : command.evidenceGroupId().trim();
        assertEvidenceGroupScope(evidenceGroupId, tenantId, groupId);

        Instant now = Instant.now(clock);
        shipmentEvidencePort.createEvidenceGroupIfAbsent(evidenceGroupId, tenantId, groupId, principal.userId(), now);

        String safeFileName = normalizeFileName(command.fileName());
        String contentType = normalizeContentType(command.contentType());
        String objectKey = buildEvidenceObjectKey(tenantId, groupId, evidenceGroupId, safeFileName);
        String evidenceId = UUID.randomUUID().toString();

        shipmentEvidencePort.createPendingEvidence(
            evidenceId,
            evidenceGroupId,
            objectKey,
            safeFileName,
            contentType,
            now
        );

        ObjectStoragePort.PresignedUpload presigned = objectStoragePort.issuePresignedUpload(objectKey, contentType, PRESIGN_TTL);
        return new PresignedShipmentEvidenceUploadResult(
            evidenceGroupId,
            evidenceId,
            objectKey,
            presigned.uploadUrl(),
            presigned.expiresAt()
        );
    }

    @Override
    @Transactional
    public ShipmentEvidenceCompleteResult completeEvidenceUpload(
        AuthPrincipal principal,
        String tenantId,
        String groupId,
        CompleteShipmentEvidenceUploadCommand command
    ) {
        authorizationSupport.assertTenantAndGroupContext(principal, tenantId, groupId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.BRAND_RELEASE, "shipment:evidence:complete");
        assertEvidenceGroupScope(command.evidenceGroupId(), tenantId, groupId);

        ShipmentEvidencePort.ShipmentEvidenceView evidence = shipmentEvidencePort.findEvidenceById(
            command.evidenceGroupId(),
            command.evidenceId()
        ).orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.EVIDENCE_NOT_FOUND, "Evidence file not found"));

        if (!objectStoragePort.objectExists(evidence.objectKey())) {
            throw new WorkflowDomainException(WorkflowErrorCode.EVIDENCE_NOT_FOUND, "Uploaded object not found");
        }
        if (command.sizeBytes() <= 0) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "sizeBytes must be positive");
        }

        String normalizedHash = normalizeHash(command.fileHash());
        shipmentEvidencePort.markEvidenceReady(
            command.evidenceGroupId(),
            command.evidenceId(),
            command.sizeBytes(),
            normalizedHash,
            Instant.now(clock)
        );

        return new ShipmentEvidenceCompleteResult(command.evidenceGroupId(), command.evidenceId(), "READY");
    }

    private void assertEvidenceGroupScope(String evidenceGroupId, String tenantId, String groupId) {
        ShipmentEvidencePort.EvidenceGroupScopeView scope = shipmentEvidencePort.findEvidenceGroupScope(evidenceGroupId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.EVIDENCE_NOT_FOUND, "Evidence group not found"));
        if (!tenantId.equals(scope.tenantId()) || !groupId.equals(scope.groupId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant/group evidence group access denied");
        }
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
        return "workflow/shipment/" + tenantId + "/" + groupId + "/" + evidenceGroupId + "/" + UUID.randomUUID() + "/" + fileName;
    }
}
