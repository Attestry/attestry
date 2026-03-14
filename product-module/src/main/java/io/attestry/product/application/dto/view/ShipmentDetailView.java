package io.attestry.product.application.dto.view;

import io.attestry.product.application.port.query.PassportShipmentQueryPort;
import java.util.List;
import java.time.Instant;

public record ShipmentDetailView(
    String shipmentId,
    String status,
    int shipmentRound,
    Instant releasedAt,
    String releasedByUserEmail,
    Instant returnedAt,
    String returnedByUserEmail,
    List<EvidenceFileView> evidenceFiles
) {
    public static ShipmentDetailView from(PassportShipmentQueryPort.ShipmentRecord view) {
        List<EvidenceFileView> files = view.evidenceFiles().stream()
            .map(e -> new EvidenceFileView(
                e.evidenceId(), e.originalFileName(), e.contentType(), e.sizeBytes(), e.downloadUrl()
            ))
            .toList();
        return new ShipmentDetailView(
            view.shipmentId(), view.status(), view.shipmentRound(),
            view.releasedAt(), view.releasedByUserEmail(),
            view.returnedAt(), view.returnedByUserEmail(),
            files
        );
    }
}
