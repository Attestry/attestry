package io.attestry.workflow.application.shipment.query;

import io.attestry.workflow.application.common.WorkflowActorContext;
import java.util.List;

public interface ShipmentQueryUseCase {

    List<ShipmentView> listByPassport(
            WorkflowActorContext principal,
            String passportId);

    PagedShipmentView listByTenant(
            WorkflowActorContext principal,
            int page,
            int size,
            String keyword);

    ShipmentDetailView getShipmentDetail(
            WorkflowActorContext principal,
            String shipmentId);

    PagedReleaseCandidateView listReleaseCandidates(
            WorkflowActorContext principal,
            int page,
            int size,
            String keyword);
}
