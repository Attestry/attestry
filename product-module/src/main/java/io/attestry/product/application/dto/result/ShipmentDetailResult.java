package io.attestry.product.application.dto.result;

import io.attestry.product.application.port.PassportShipmentQueryPort;
import java.time.Instant;
import java.util.List;

public record ShipmentDetailResult(
    String shipmentId,
    String status,
    int shipmentRound,
    Instant releasedAt,
    String releasedByUserEmail,
    Instant returnedAt,
    String returnedByUserEmail,
    List<EvidenceFileResult> evidenceFiles
) {
    public static ShipmentDetailResult from(PassportShipmentQueryPort.ShipmentView view) {
        List<EvidenceFileResult> files = view.evidenceFiles().stream()
            .map(e -> new EvidenceFileResult(
                e.evidenceId(), e.originalFileName(), e.contentType(), e.sizeBytes(), e.downloadUrl()
            ))
            .toList();
        return new ShipmentDetailResult(
            view.shipmentId(), view.status(), view.shipmentRound(),
            view.releasedAt(), view.releasedByUserEmail(),
            view.returnedAt(), view.returnedByUserEmail(),
            files
        );
    }
}
