package io.attestry.workflow.application.claim.command;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.shipment.command.PresignedEvidenceUploadResult;
import io.attestry.workflow.application.shipment.command.EvidenceCompleteResult;
import io.attestry.workflow.application.EvidenceProperties;
import io.attestry.workflow.application.support.EvidenceUploadSupport;
import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurchaseClaimEvidenceService {

    private static final String OBJECT_KEY_PREFIX = "workflow/purchase-claim/";

    private final EvidenceProperties evidenceProperties;
    private final WorkflowEvidencePort evidencePort;
    private final ObjectStoragePort objectStoragePort;
    private final EvidenceUploadSupport evidenceUploadSupport;
    private final Clock clock;


    @Transactional
    public PresignedEvidenceUploadResult presignEvidence(
            WorkflowActorContext principal,
            PresignClaimEvidenceCommand command) {
        String tenantId = resolveTenantId(principal);
        return evidenceUploadSupport.doPresign(
                evidencePort, objectStoragePort,
                OBJECT_KEY_PREFIX, evidenceProperties.getPresignTtl(),
                tenantId, principal.userId(),
                command.evidenceGroupId(), command.fileName(), command.contentType(),
                Instant.now(clock));
    }

    @Transactional
    public EvidenceCompleteResult completeEvidence(
            WorkflowActorContext principal,
            CompleteClaimEvidenceCommand command) {
        String tenantId = resolveTenantId(principal);
        evidenceUploadSupport.assertEvidenceGroupScope(evidencePort, command.evidenceGroupId(), tenantId);
        return evidenceUploadSupport.doComplete(
                evidencePort, objectStoragePort,
                command.evidenceGroupId(), command.evidenceId(),
                command.sizeBytes(), command.fileHash(), Instant.now(clock));
    }

    private String resolveTenantId(WorkflowActorContext principal) {
        if (principal.tenantId() == null || principal.tenantId().isBlank()) {
            return "CONSUMER"; // B2C users don't have a tenantId
        }
        return principal.tenantId();
    }
}
