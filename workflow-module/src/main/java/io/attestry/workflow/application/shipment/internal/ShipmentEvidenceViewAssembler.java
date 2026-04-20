package io.attestry.workflow.application.shipment.internal;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.workflow.application.EvidenceProperties;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.port.shipment.WorkflowShipmentHistoryPort.EvidenceFileRecord;
import io.attestry.workflow.application.shipment.query.ShipmentDetailView.EvidenceFileView;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShipmentEvidenceViewAssembler {

    private final EvidenceProperties evidenceProperties;
    private final WorkflowEvidencePort evidencePort;
    private final ObjectStoragePort objectStoragePort;


    public List<EvidenceFileView> toDetailEvidenceFiles(String evidenceGroupId) {
        return toPassportEvidenceRecords(evidenceGroupId).stream()
            .map(file -> new EvidenceFileView(
                file.evidenceId(),
                file.originalFileName(),
                file.contentType(),
                file.sizeBytes(),
                file.downloadUrl()
            ))
            .toList();
    }

    public List<EvidenceFileRecord> toPassportEvidenceRecords(String evidenceGroupId) {
        if (evidenceGroupId == null) {
            return List.of();
        }
        return evidencePort.findEvidenceByEvidenceGroupId(evidenceGroupId).stream()
            .filter(e -> "READY".equals(e.status()))
            .map(e -> new EvidenceFileRecord(
                e.evidenceId(),
                e.originalFileName(),
                e.contentType(),
                e.sizeBytes(),
                e.objectKey() != null
                    ? objectStoragePort.issuePresignedDownload(e.objectKey(), evidenceProperties.getDownloadTtl()).downloadUrl()
                    : null
            ))
            .toList();
    }
}
