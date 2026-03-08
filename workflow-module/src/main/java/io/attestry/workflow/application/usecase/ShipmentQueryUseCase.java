package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.shipment.result.EvidenceViewResult;
import io.attestry.workflow.application.shipment.result.ShipmentViewResult;
import java.util.List;

public interface ShipmentQueryUseCase {

    List<ShipmentViewResult> listByPassport(
        AuthPrincipal principal,
        String tenantId,
        String passportId
    );

    List<EvidenceViewResult> listEvidenceByShipmentId(
        AuthPrincipal principal,
        String tenantId,
        String shipmentId
    );
}
