package io.attestry.workflow.application.servicerequest.assembler;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.usecase.ServiceRequestQueryUseCase.EvidenceFileResult;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ServiceRequestEvidenceAssembler {

    private static final Duration DOWNLOAD_TTL = Duration.ofMinutes(30);

    private final WorkflowEvidencePort evidencePort;
    private final ObjectStoragePort objectStoragePort;

    public ServiceRequestEvidenceAssembler(
        WorkflowEvidencePort evidencePort,
        ObjectStoragePort objectStoragePort
    ) {
        this.evidencePort = evidencePort;
        this.objectStoragePort = objectStoragePort;
    }

    public List<EvidenceFileResult> toEvidenceFiles(String evidenceGroupId) {
        if (evidenceGroupId == null || evidenceGroupId.isBlank()) {
            return List.of();
        }
        return evidencePort.findEvidenceByEvidenceGroupId(evidenceGroupId).stream()
            .filter(e -> "READY".equals(e.status()))
            .map(e -> new EvidenceFileResult(
                e.evidenceId(),
                e.originalFileName(),
                e.contentType(),
                e.sizeBytes(),
                e.objectKey() == null ? null : objectStoragePort.issuePresignedDownload(e.objectKey(), DOWNLOAD_TTL).downloadUrl()
            ))
            .toList();
    }
}
