package io.attestry.notification;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.userauth.application.port.notification.PassportManualNotificationPort;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PassportManualAttachmentResolver {

    private static final Duration DOWNLOAD_URL_TTL = Duration.ofDays(7);

    private final WorkflowEvidencePort workflowEvidencePort;
    private final ObjectStoragePort objectStoragePort;

    public PassportManualAttachmentResolver(
        WorkflowEvidencePort workflowEvidencePort,
        ObjectStoragePort objectStoragePort
    ) {
        this.workflowEvidencePort = workflowEvidencePort;
        this.objectStoragePort = objectStoragePort;
    }

    public List<PassportManualNotificationPort.ManualAttachment> resolveAttachments(
        PassportManualNotificationPort.PassportManualNotification notification
    ) {
        if (notification.evidenceGroupId() == null || notification.evidenceGroupId().isBlank()) {
            return List.of();
        }

        Set<String> allowedEvidenceIds = notification.attachmentEvidenceIds() == null
            ? Set.of()
            : notification.attachmentEvidenceIds().stream().collect(Collectors.toSet());

        return workflowEvidencePort.findEvidenceByEvidenceGroupId(notification.evidenceGroupId()).stream()
            .filter(evidence -> "READY".equalsIgnoreCase(evidence.status()))
            .filter(evidence -> allowedEvidenceIds.isEmpty() || allowedEvidenceIds.contains(evidence.evidenceId()))
            .map(evidence -> new PassportManualNotificationPort.ManualAttachment(
                evidence.evidenceId(),
                evidence.originalFileName(),
                objectStoragePort.issuePresignedDownload(evidence.objectKey(), DOWNLOAD_URL_TTL).downloadUrl()
            ))
            .toList();
    }
}
