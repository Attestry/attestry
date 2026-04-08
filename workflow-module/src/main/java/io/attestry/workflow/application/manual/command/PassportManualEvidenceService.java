package io.attestry.workflow.application.manual.command;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.shipment.command.CompleteShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.PresignShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.result.EvidenceCompleteResult;
import io.attestry.workflow.application.shipment.result.PresignedEvidenceUploadResult;
import io.attestry.workflow.application.support.EvidenceUploadSupport;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PassportManualEvidenceService implements PassportManualEvidenceUseCase {

    private static final String OBJECT_KEY_PREFIX = "workflow/passport-manual/";
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
        String tenantId,
        PresignShipmentEvidenceUploadCommand command
    ) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.BRAND_RELEASE, "passport-manual:evidence:presign");

        return evidenceUploadSupport.doPresign(
            evidencePort,
            objectStoragePort,
            OBJECT_KEY_PREFIX,
            PRESIGN_TTL,
            tenantId,
            principal.userId(),
            command.evidenceGroupId(),
            command.fileName(),
            command.contentType(),
            Instant.now(clock)
        );
    }

    @Override
    @Transactional
    public EvidenceCompleteResult completeEvidenceUpload(
        WorkflowActorContext principal,
        String tenantId,
        CompleteShipmentEvidenceUploadCommand command
    ) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.BRAND_RELEASE, "passport-manual:evidence:complete");
        return evidenceUploadSupport.doComplete(
            evidencePort,
            objectStoragePort,
            command.evidenceGroupId(),
            command.evidenceId(),
            command.sizeBytes(),
            command.fileHash(),
            Instant.now(clock)
        );
    }
}
