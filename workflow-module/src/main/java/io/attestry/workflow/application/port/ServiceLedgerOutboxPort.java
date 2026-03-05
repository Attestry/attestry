package io.attestry.workflow.application.port;

import io.attestry.workflow.application.shipment.result.WorkflowLedgerEventEnvelope;

public interface ServiceLedgerOutboxPort {

    String enqueue(WorkflowLedgerEventEnvelope event);
}
