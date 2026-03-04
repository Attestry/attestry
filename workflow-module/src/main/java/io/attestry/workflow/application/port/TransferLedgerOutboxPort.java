package io.attestry.workflow.application.port;

import io.attestry.workflow.application.shipment.result.WorkflowLedgerEventEnvelope;

public interface TransferLedgerOutboxPort {

    String enqueue(WorkflowLedgerEventEnvelope event);
}
