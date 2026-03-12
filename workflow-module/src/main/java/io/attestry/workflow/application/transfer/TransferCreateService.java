package io.attestry.workflow.application.transfer;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.transfer.policy.TransferAccessPolicy;
import io.attestry.workflow.application.transfer.support.TransferContextResolver;
import io.attestry.workflow.application.transfer.command.CreateB2CTransferCommand;
import io.attestry.workflow.application.transfer.command.CreateC2CTransferCommand;
import io.attestry.workflow.application.transfer.result.CreateTransferResult;
import io.attestry.workflow.domain.transfer.policy.TransferCreatePolicy.TransferCreateContext;
import io.attestry.workflow.application.usecase.TransferCreateUseCase;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferType;
import io.attestry.workflow.domain.transfer.repository.TokenTransferRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferCreateService implements TransferCreateUseCase {

    private final TokenTransferRepository transferRepository;
    private final TransferAccessPolicy accessPolicy;
    private final TransferContextResolver contextResolver;
    private final TransferCreateExecutor createExecutor;
    private final Clock clock;

    @Override
    @Transactional
    public CreateTransferResult createC2C(
        AuthPrincipal principal,
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
        AuthPrincipal principal,
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
        AuthPrincipal principal,
        String passportId
    ) {
        accessPolicy.assertFindPendingC2CAccess(principal, passportId);
        return findLatestActivePending(passportId).map(this::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CreateTransferResult> findLatestActivePendingB2CByPassportId(
        AuthPrincipal principal,
        String tenantId,
        String passportId
    ) {
        accessPolicy.assertFindPendingB2CAccess(principal, tenantId, passportId);
        return findLatestActivePending(passportId, TransferType.B2C, tenantId)
            .map(this::toResult);
    }

    private Optional<TokenTransfer> findLatestActivePending(String passportId) {
        return findLatestActivePending(passportId, null, null);
    }

    private Optional<TokenTransfer> findLatestActivePending(
        String passportId,
        TransferType transferType,
        String tenantId
    ) {
        return transferRepository.findLatestActivePendingByPassportId(
            passportId,
            Instant.now(clock),
            transferType,
            tenantId
        );
    }

    private CreateTransferResult toResult(TokenTransfer saved) {
        return new CreateTransferResult(
            saved.transferId(),
            saved.passportId(),
            saved.transferType().name(),
            saved.status().name(),
            saved.acceptMethod().name(),
            saved.qrNonce(),
            saved.expiresAt()
        );
    }
}
