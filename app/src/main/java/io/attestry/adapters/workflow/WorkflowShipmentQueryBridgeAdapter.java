package io.attestry.adapters.workflow;

import io.attestry.product.application.port.query.PassportShipmentQueryPort;
import io.attestry.workflow.application.port.shipment.WorkflowShipmentHistoryPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class WorkflowShipmentQueryBridgeAdapter implements PassportShipmentQueryPort {

    private final WorkflowShipmentHistoryPort workflowShipmentHistoryPort;

    public WorkflowShipmentQueryBridgeAdapter(WorkflowShipmentHistoryPort workflowShipmentHistoryPort) {
        this.workflowShipmentHistoryPort = workflowShipmentHistoryPort;
    }

    @Override
    public Optional<ShipmentRecord> findLatestShipmentByPassportId(String passportId) {
        return workflowShipmentHistoryPort.findLatestShipmentByPassportId(passportId)
            .map(record -> new ShipmentRecord(
                record.shipmentId(),
                record.status(),
                record.shipmentRound(),
                record.releasedAt(),
                record.releasedByUserEmail(),
                record.returnedAt(),
                record.returnedByUserEmail(),
                record.evidenceFiles().stream()
                    .map(file -> new EvidenceFileRecord(
                        file.evidenceId(),
                        file.originalFileName(),
                        file.contentType(),
                        file.sizeBytes(),
                        file.downloadUrl()
                    ))
                    .toList()
            ));
    }
}
