package io.attestry.workflow.application.transfer.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.transfer.internal.TransferAccessPolicy;
import io.attestry.workflow.application.transfer.internal.TransferContextResolver;
import io.attestry.workflow.application.transfer.result.CreateTransferResult;
import io.attestry.workflow.application.transfer.internal.TransferCreateResultMapper;
import io.attestry.workflow.application.transfer.internal.TransferCreateExecutor;
import io.attestry.workflow.application.transfer.internal.TransferLookupService;
import io.attestry.workflow.domain.transfer.policy.TransferCreatePolicy.TransferCreateContext;
import io.attestry.workflow.domain.transfer.model.TransferType;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferCreateService implements TransferCreateUseCase {

    private final TransferAccessPolicy accessPolicy;
    private final TransferContextResolver contextResolver;
    private final TransferCreateExecutor createExecutor;
    private final TransferLookupService transferLookupService;
    private final TransferCreateResultMapper resultMapper;
    private final Clock clock;

    @Override
    @Transactional
    public CreateTransferResult createC2C(
        WorkflowActorContext principal,
        String passportId,
        CreateC2CTransferCommand command
    ) {
        accessPolicy.assertCreateC2CAccess(principal, passportId);
        TransferCreateContext context = contextResolver.resolveCreateContext(principal.userId(), null, passportId);
        return createExecutor.createC2C(passportId, principal.userId(), command, context);
    }

    @Override
    @Transactional
    public CreateTransferResult createB2C(
        WorkflowActorContext principal,
        String tenantId,
        String passportId,
        CreateB2CTransferCommand command
    ) {
        accessPolicy.assertCreateB2CAccess(principal, tenantId, passportId);
        TransferCreateContext context = contextResolver.resolveCreateContext(principal.userId(), tenantId, passportId);
        return createExecutor.createB2C(passportId, tenantId, principal.userId(), command, context);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CreateTransferResult> findLatestActivePendingByPassportId(
        WorkflowActorContext principal,
        String passportId
    ) {
        accessPolicy.assertFindPendingC2CAccess(principal, passportId);
        return findLatestActivePending(passportId).map(resultMapper::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CreateTransferResult> findLatestActivePendingB2CByPassportId(
        WorkflowActorContext principal,
        String tenantId,
        String passportId
    ) {
        accessPolicy.assertFindPendingB2CAccess(principal, tenantId, passportId);
        return findLatestActivePending(passportId, TransferType.B2C, tenantId).map(resultMapper::toResult);
    }

    private Optional<io.attestry.workflow.domain.transfer.model.TokenTransfer> findLatestActivePending(String passportId) {
        return findLatestActivePending(passportId, null, null);
    }

    private Optional<io.attestry.workflow.domain.transfer.model.TokenTransfer> findLatestActivePending(
        String passportId,
        TransferType transferType,
        String tenantId
    ) {
        return transferLookupService.findLatestActivePendingByPassportId(
            passportId,
            Instant.now(clock),
            transferType,
            tenantId
        );
    }
}
