package io.attestry.workflow.application.servicerequest;

import io.attestry.userauth.application.port.ObjectStoragePort;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.ShipmentEvidencePort;
import io.attestry.workflow.application.shipment.command.CompleteShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.PresignShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.result.PresignedShipmentEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.ShipmentEvidenceCompleteResult;
import io.attestry.workflow.application.support.EvidenceUploadSupport;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.ServiceEvidenceUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceEvidenceService implements ServiceEvidenceUseCase {

    private static final String OBJECT_KEY_PREFIX = "workflow/service/";
    private static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

    private final ShipmentEvidencePort shipmentEvidencePort;
    private final ObjectStoragePort objectStoragePort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final EvidenceUploadSupport evidenceUploadSupport;
    private final Clock clock;

    public ServiceEvidenceService(
        ShipmentEvidencePort shipmentEvidencePort,
        ObjectStoragePort objectStoragePort,
        WorkflowAuthorizationSupport authorizationSupport,
        EvidenceUploadSupport evidenceUploadSupport,
        Clock clock
    ) {
        this.shipmentEvidencePort = shipmentEvidencePort;
        this.objectStoragePort = objectStoragePort;
        this.authorizationSupport = authorizationSupport;
        this.evidenceUploadSupport = evidenceUploadSupport;
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
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.SERVICE_COMPLETE, "service:evidence:presign");

        return doPresign(principal, tenantId, groupId, command);
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
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.SERVICE_COMPLETE, "service:evidence:complete");

        return doComplete(command);
    }

    @Override
    @Transactional
    public PresignedShipmentEvidenceUploadResult presignOwnerEvidenceUpload(
        AuthPrincipal principal,
        PresignShipmentEvidenceUploadCommand command
    ) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_SERVICE_CREATE, "service:owner-evidence:presign");

        String tenantId = principal.tenantId() != null ? principal.tenantId() : "owner";
        String groupId = principal.groupId() != null ? principal.groupId() : "owner";
        return doPresign(principal, tenantId, groupId, command);
    }

    @Override
    @Transactional
    public ShipmentEvidenceCompleteResult completeOwnerEvidenceUpload(
        AuthPrincipal principal,
        CompleteShipmentEvidenceUploadCommand command
    ) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_SERVICE_CREATE, "service:owner-evidence:complete");

        return doComplete(command);
    }

    private PresignedShipmentEvidenceUploadResult doPresign(
        AuthPrincipal principal,
        String tenantId,
        String groupId,
        PresignShipmentEvidenceUploadCommand command
    ) {
        String evidenceGroupId = command.evidenceGroupId() == null || command.evidenceGroupId().isBlank()
            ? UUID.randomUUID().toString()
            : command.evidenceGroupId().trim();

        Instant now = Instant.now(clock);
        shipmentEvidencePort.createEvidenceGroupIfAbsent(evidenceGroupId, tenantId, groupId, principal.userId(), now);

        String safeFileName = evidenceUploadSupport.normalizeFileName(command.fileName());
        String contentType = evidenceUploadSupport.normalizeContentType(command.contentType());
        String objectKey = evidenceUploadSupport.buildObjectKey(OBJECT_KEY_PREFIX, tenantId, groupId, evidenceGroupId, safeFileName);
        String evidenceId = UUID.randomUUID().toString();

        shipmentEvidencePort.createPendingEvidence(evidenceId, evidenceGroupId, objectKey, safeFileName, contentType, now);

        ObjectStoragePort.PresignedUpload presigned = objectStoragePort.issuePresignedUpload(objectKey, contentType, PRESIGN_TTL);
        return new PresignedShipmentEvidenceUploadResult(
            evidenceGroupId, evidenceId, objectKey, presigned.uploadUrl(), presigned.expiresAt()
        );
    }

    private ShipmentEvidenceCompleteResult doComplete(CompleteShipmentEvidenceUploadCommand command) {
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
