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

    PagedShipmentViewResponse listByTenant(
            AuthPrincipal principal,
            int page,
            int size,
            String keyword);

    ShipmentDetailResult getShipmentDetail(
            AuthPrincipal principal,
            String shipmentId);

    PagedReleaseCandidateResponse listReleaseCandidates(
            AuthPrincipal principal,
            int page,
            int size,
            String keyword);

    record PagedShipmentViewResponse(
            List<ShipmentViewResult> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }

    record PagedReleaseCandidateResponse(
            List<ShipmentReleaseCandidateResult> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }
}
