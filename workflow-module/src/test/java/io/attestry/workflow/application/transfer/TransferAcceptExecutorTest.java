package io.attestry.workflow.application.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.workflow.application.port.common.WorkflowLedgerOutboxPort;
import io.attestry.workflow.application.port.transfer.TransferOwnershipUpdatePort;
import io.attestry.workflow.application.transfer.command.AcceptTransferCommand;
import io.attestry.workflow.application.transfer.result.AcceptTransferResult;
import io.attestry.workflow.application.usecase.DelegationLifecycleUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.transfer.model.AcceptCredential;
import io.attestry.workflow.domain.transfer.model.AcceptMethod;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferStatus;
import io.attestry.workflow.domain.transfer.policy.TransferAcceptPolicy;
import io.attestry.workflow.domain.transfer.policy.TransferAcceptPolicy.TransferAcceptContext;
import io.attestry.workflow.domain.transfer.repository.TokenTransferRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferAcceptExecutorTest {

    private static final Instant NOW = Instant.parse("2026-03-12T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock TokenTransferRepository transferRepository;
    @Mock TransferOwnershipUpdatePort ownershipUpdatePort;
    @Mock WorkflowLedgerOutboxPort outboxPort;
    @Mock DelegationLifecycleUseCase delegationLifecycleUseCase;
    @Mock TransferHashSupport hashSupport;
    @Mock TransferAcceptPolicy acceptPolicy;

    private TransferAcceptExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new TransferAcceptExecutor(
            transferRepository,
            ownershipUpdatePort,
            outboxPort,
            delegationLifecycleUseCase,
            hashSupport,
            acceptPolicy,
            CLOCK
        );
    }

    @Test
    void accept_c2cQr_completesAndUpdatesOwnershipWithoutDelegationConsume() {
        TokenTransfer pending = pendingC2CQr();
        TokenTransfer completed = pending.complete("consumer-1", NOW);
        TransferAcceptContext context = new TransferAcceptContext(true, "NONE", "owner-1", "owner-1");

        when(transferRepository.findById("t1")).thenReturn(Optional.of(pending));
        doNothing().when(acceptPolicy).assertAcceptable(context);
        when(transferRepository.save(any(TokenTransfer.class))).thenReturn(completed);
        when(outboxPort.enqueue(any())).thenReturn("outbox-1");

        AcceptTransferResult result = executor.accept(
            "consumer-1",
            new AcceptTransferCommand("nonce-1", null),
            context,
            "t1"
        );

        assertEquals("COMPLETED", result.status());
        assertEquals("consumer-1", result.toOwnerId());
        assertEquals("outbox-1", result.outboxEventId());
        verify(acceptPolicy).assertAcceptable(context);
        verify(ownershipUpdatePort).upsertOwner("p1", "consumer-1", NOW);
        verify(outboxPort).enqueue(any());
        verify(delegationLifecycleUseCase, never()).consumeByPassportId(any());
    }

    @Test
    void accept_b2cQr_consumesDelegationAfterOwnershipUpdate() {
        TokenTransfer pending = pendingB2CQr();
        TokenTransfer completed = pending.complete("consumer-1", NOW);
        TransferAcceptContext context = new TransferAcceptContext(false, "NONE", null, null);

        when(transferRepository.findById("t2")).thenReturn(Optional.of(pending));
        doNothing().when(acceptPolicy).assertAcceptable(context);
        when(transferRepository.save(any(TokenTransfer.class))).thenReturn(completed);
        when(outboxPort.enqueue(any())).thenReturn("outbox-2");

        AcceptTransferResult result = executor.accept(
            "consumer-1",
            new AcceptTransferCommand("nonce-b2c", null),
            context,
            "t2"
        );

        assertEquals("COMPLETED", result.status());
        verify(ownershipUpdatePort).upsertOwner("p2", "consumer-1", NOW);
        verify(delegationLifecycleUseCase).consumeByPassportId("p2");
        verify(outboxPort).enqueue(any());
    }

    @Test
    void accept_qrNonceMismatch_throwsAndDoesNotPersistCompletion() {
        TokenTransfer pending = pendingC2CQr();
        TransferAcceptContext context = new TransferAcceptContext(true, "NONE", "owner-1", "owner-1");

        when(transferRepository.findById("t1")).thenReturn(Optional.of(pending));
        doNothing().when(acceptPolicy).assertAcceptable(context);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            executor.accept("consumer-1", new AcceptTransferCommand("wrong-nonce", null), context, "t1")
        );

        assertEquals(WorkflowErrorCode.TRANSFER_NONCE_MISMATCH, ex.getErrorCode());
        verify(transferRepository, never()).save(any(TokenTransfer.class));
        verify(ownershipUpdatePort, never()).upsertOwner(any(), any(), any());
        verify(outboxPort, never()).enqueue(any());
    }

    @Test
    void accept_codeMismatch_incrementsAttemptAndThrows() {
        TokenTransfer pending = pendingB2CCode(0);
        TokenTransfer incremented = pending.incrementAttempt();
        TransferAcceptContext context = new TransferAcceptContext(false, "NONE", null, null);

        when(transferRepository.findById("t3")).thenReturn(Optional.of(pending));
        doNothing().when(acceptPolicy).assertAcceptable(context);
        when(transferRepository.save(any(TokenTransfer.class))).thenReturn(incremented);
        when(hashSupport.verify("wrong-code", incremented.codeHash(), incremented.codeSalt())).thenReturn(false);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            executor.accept("consumer-1", new AcceptTransferCommand(null, "wrong-code"), context, "t3")
        );

        assertEquals(WorkflowErrorCode.TRANSFER_CODE_MISMATCH, ex.getErrorCode());
        verify(transferRepository).save(eq(incremented));
        verify(ownershipUpdatePort, never()).upsertOwner(any(), any(), any());
        verify(outboxPort, never()).enqueue(any());
    }

    @Test
    void accept_codeMismatchWhenBlocked_cancelsTransferAndThrows() {
        TokenTransfer pending = pendingB2CCode(4);
        TokenTransfer incremented = pending.incrementAttempt();
        TokenTransfer cancelled = incremented.cancel(null, NOW);
        TransferAcceptContext context = new TransferAcceptContext(false, "NONE", null, null);

        when(transferRepository.findById("t3")).thenReturn(Optional.of(pending));
        doNothing().when(acceptPolicy).assertAcceptable(context);
        when(transferRepository.save(any(TokenTransfer.class)))
            .thenReturn(incremented)
            .thenReturn(cancelled);
        when(hashSupport.verify("wrong-code", incremented.codeHash(), incremented.codeSalt())).thenReturn(false);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            executor.accept("consumer-1", new AcceptTransferCommand(null, "wrong-code"), context, "t3")
        );

        assertEquals(WorkflowErrorCode.TRANSFER_CODE_MISMATCH, ex.getErrorCode());
        verify(transferRepository, times(2)).save(any(TokenTransfer.class));
        verify(transferRepository).save(eq(incremented));
        verify(transferRepository).save(eq(cancelled));
        verify(ownershipUpdatePort, never()).upsertOwner(any(), any(), any());
        verify(outboxPort, never()).enqueue(any());
    }

    private TokenTransfer pendingC2CQr() {
        return TokenTransfer.createC2C(
            "t1",
            "p1",
            "owner-1",
            AcceptCredential.ofQr("nonce-1"),
            NOW.plusSeconds(3600),
            NOW.minusSeconds(60),
            "owner-1"
        );
    }

    private TokenTransfer pendingB2CQr() {
        return TokenTransfer.createB2C(
            "t2",
            "p2",
            "tenant-1",
            AcceptCredential.ofQr("nonce-b2c"),
            NOW.plusSeconds(3600),
            NOW.minusSeconds(60),
            "retail-user-1"
        );
    }

    private TokenTransfer pendingB2CCode(int attempts) {
        TokenTransfer transfer = TokenTransfer.createB2C(
            "t3",
            "p3",
            "tenant-1",
            AcceptCredential.ofCode("hash-1", "salt-1"),
            NOW.plusSeconds(3600),
            NOW.minusSeconds(60),
            "retail-user-1"
        );
        for (int i = 0; i < attempts; i++) {
            transfer = transfer.incrementAttempt();
        }
        return transfer;
    }
}
