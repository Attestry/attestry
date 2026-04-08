package io.attestry.workflow.application.shipment.command;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.shipment.result.PresignedEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.EvidenceCompleteResult;
import io.attestry.workflow.application.support.EvidenceUploadSupport;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ShipmentEvidenceService implements ShipmentEvidenceUseCase {

    private static final String OBJECT_KEY_PREFIX = "workflow/shipment/";
    private static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

    private final WorkflowEvidencePort evidencePort;
    private final ObjectStoragePort objectStoragePort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final EvidenceUploadSupport evidenceUploadSupport;
    private final Clock clock;

    @Override
    @Transactional
    public PresignedEvidenceUploadResult presignEvidenceUpload(
        WorkflowActorContext principal,
        PresignShipmentEvidenceUploadCommand command
    ) {
        String tenantId = principal.tenantId();
        assertEvidenceWriteAccess(principal, tenantId, "shipment:evidence:presign");

        return evidenceUploadSupport.doPresign(
            evidencePort, objectStoragePort,
            OBJECT_KEY_PREFIX, PRESIGN_TTL,
            tenantId, principal.userId(),
            command.evidenceGroupId(), command.fileName(), command.contentType(),
            Instant.now(clock)
        );
    }

    @Override
    @Transactional
    public EvidenceCompleteResult completeEvidenceUpload(
        WorkflowActorContext principal,
        CompleteShipmentEvidenceUploadCommand command
    ) {
        String tenantId = principal.tenantId();
        assertEvidenceWriteAccess(principal, tenantId, "shipment:evidence:complete");
        evidenceUploadSupport.assertEvidenceGroupScope(evidencePort, command.evidenceGroupId(), tenantId);

        return evidenceUploadSupport.doComplete(
            evidencePort, objectStoragePort,
            command.evidenceGroupId(), command.evidenceId(),
            command.sizeBytes(), command.fileHash(), Instant.now(clock)
        );
    }

    private void assertEvidenceWriteAccess(WorkflowActorContext principal, String tenantId, String resourceRef) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(
            principal,
            tenantId,
            PermissionCodes.BRAND_RELEASE,
            resourceRef
        );
    }
}
