package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.shipment.result.ShipmentDetailResult;
import io.attestry.workflow.application.shipment.result.ShipmentReleaseCandidateResult;
import io.attestry.workflow.application.shipment.result.ShipmentViewResult;
import java.util.List;

public interface ShipmentQueryUseCase {

    List<ShipmentViewResult> listByPassport(
            AuthPrincipal principal,
            String passportId);

    List<ShipmentViewResult> listByTenant(
            AuthPrincipal principal);

    ShipmentDetailResult getShipmentDetail(
            AuthPrincipal principal,
            String shipmentId);

    List<ShipmentReleaseCandidateResult> listReleaseCandidates(
            AuthPrincipal principal);
}
