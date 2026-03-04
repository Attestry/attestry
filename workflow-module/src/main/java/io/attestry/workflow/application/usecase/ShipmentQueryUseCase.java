package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.shipment.result.ShipmentEvidenceViewResult;
import io.attestry.workflow.application.shipment.result.ShipmentViewResult;
import java.util.List;

public interface ShipmentQueryUseCase {

    List<ShipmentViewResult> listByPassport(
        AuthPrincipal principal,
        String tenantId,
        String groupId,
        String passportId
    );

    List<ShipmentEvidenceViewResult> listEvidenceByShipmentId(
        AuthPrincipal principal,
        String tenantId,
        String groupId,
        String shipmentId
    );
}
