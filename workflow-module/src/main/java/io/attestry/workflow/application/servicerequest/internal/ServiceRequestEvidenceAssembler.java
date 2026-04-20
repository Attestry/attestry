package io.attestry.workflow.application.servicerequest.internal;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.workflow.application.EvidenceProperties;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.servicerequest.view.ServiceRequestEvidenceFileView;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ServiceRequestEvidenceAssembler {

    private final EvidenceProperties evidenceProperties;
    private final WorkflowEvidencePort evidencePort;
    private final ObjectStoragePort objectStoragePort;

    public ServiceRequestEvidenceAssembler(
        EvidenceProperties evidenceProperties,
        WorkflowEvidencePort evidencePort,
        ObjectStoragePort objectStoragePort
    ) {
        this.evidenceProperties = evidenceProperties;
        this.evidencePort = evidencePort;
        this.objectStoragePort = objectStoragePort;
    }

    public List<ServiceRequestEvidenceFileView> toEvidenceFiles(String evidenceGroupId) {
        if (evidenceGroupId == null || evidenceGroupId.isBlank()) {
            return List.of();
        }
        return evidencePort.findEvidenceByEvidenceGroupId(evidenceGroupId).stream()
            .filter(e -> "READY".equals(e.status()))
            .map(e -> new ServiceRequestEvidenceFileView(
                e.evidenceId(),
                e.originalFileName(),
                e.contentType(),
                e.sizeBytes(),
                e.objectKey() == null ? null : objectStoragePort.issuePresignedDownload(e.objectKey(), evidenceProperties.getDownloadTtl()).downloadUrl()
            ))
            .toList();
    }
}
