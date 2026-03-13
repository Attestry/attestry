package io.attestry.workflow.application.port.common;

import io.attestry.workflow.application.shipment.result.WorkflowLedgerEventEnvelope;

public interface WorkflowLedgerOutboxPort {

    String enqueue(WorkflowLedgerEventEnvelope event);
}
