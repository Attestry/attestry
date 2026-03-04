package io.attestry.workflow.application.port;

import io.attestry.workflow.application.shipment.result.WorkflowLedgerEventEnvelope;

public interface ShipmentLedgerOutboxPort {

    String enqueue(WorkflowLedgerEventEnvelope event);
}
