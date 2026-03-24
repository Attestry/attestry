package io.attestry.workflow.application.transfer.usecase;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.transfer.view.PagedCompletedTransferView;

public interface TransferQueryUseCase {

    PagedCompletedTransferView listCompletedB2CTransfers(
        WorkflowActorContext principal,
        String tenantId,
        String sourceTenantId,
        int page,
        int size
    );
}
