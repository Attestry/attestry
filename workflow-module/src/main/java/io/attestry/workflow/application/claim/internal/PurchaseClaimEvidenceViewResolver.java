package io.attestry.workflow.application.claim.internal;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.workflow.application.claim.view.ClaimEvidenceView;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseClaimEvidenceViewResolver {

    private static final Duration DOWNLOAD_TTL = Duration.ofDays(3);
    private static final String READY_STATUS = "READY";

    private final WorkflowEvidencePort evidencePort;
    private final ObjectStoragePort objectStoragePort;

    public List<ClaimEvidenceView> resolveReadyEvidenceViews(String evidenceGroupId) {
        return evidencePort.findEvidenceByEvidenceGroupId(evidenceGroupId).stream()
            .filter(evidence -> READY_STATUS.equalsIgnoreCase(evidence.status()))
            .map(this::toView)
            .toList();
    }

    private ClaimEvidenceView toView(WorkflowEvidencePort.EvidenceRecord evidence) {
        ObjectStoragePort.PresignedDownload download = objectStoragePort.issuePresignedDownload(
            evidence.objectKey(),
            DOWNLOAD_TTL
        );
        return new ClaimEvidenceView(
            evidence.evidenceId(),
            evidence.status(),
            download.downloadUrl(),
            download.expiresAt()
        );
    }
}
