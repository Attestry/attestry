package io.attestry.workflow.application.manual.internal;

import io.attestry.userauth.domain.membership.model.PassportManualNotificationPayload.AttachmentPayload;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.support.EvidenceUploadSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("workflowPassportManualAttachmentResolver")
@RequiredArgsConstructor
public class PassportManualAttachmentResolver {

    private static final String READY_EVIDENCE_STATUS = "READY";

    private final WorkflowEvidencePort evidencePort;
    private final EvidenceUploadSupport evidenceUploadSupport;

    public List<AttachmentPayload> resolve(String evidenceGroupId, String tenantId) {
        evidenceUploadSupport.assertEvidenceGroupScope(evidencePort, evidenceGroupId, tenantId);
        List<AttachmentPayload> attachments = evidencePort.findEvidenceByEvidenceGroupId(evidenceGroupId).stream()
            .filter(evidence -> READY_EVIDENCE_STATUS.equalsIgnoreCase(evidence.status()))
            .map(evidence -> new AttachmentPayload(
                evidence.evidenceId(),
                evidence.originalFileName(),
                evidence.objectKey(),
                evidence.contentType()
            ))
            .toList();
        if (attachments.isEmpty()) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.PASSPORT_MANUAL_CONTENT_REQUIRED,
                "첨부 파일을 다시 확인해주세요."
            );
        }
        return attachments;
    }
}
