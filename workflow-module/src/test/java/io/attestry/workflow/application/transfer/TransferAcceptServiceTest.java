package io.attestry.workflow.application.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.userauth.domain.auth.model.VerificationLevel;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.transfer.command.TransferAcceptService;
import io.attestry.workflow.application.transfer.support.TransferAcceptExecutor;
import io.attestry.workflow.application.transfer.command.AcceptTransferCommand;
import io.attestry.workflow.application.transfer.policy.TransferAccessPolicy;
import io.attestry.workflow.application.transfer.result.AcceptTransferResult;
import io.attestry.workflow.application.transfer.support.TransferContextResolver;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.transfer.policy.TransferAcceptPolicy.TransferAcceptContext;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferAcceptServiceTest {

    @Mock TransferAccessPolicy accessPolicy;
    @Mock TransferContextResolver contextResolver;
    @Mock TransferAcceptExecutor acceptExecutor;

    private TransferAcceptService service;

    private static final WorkflowActorContext CONSUMER = new WorkflowActorContext(
        "token1", "consumer1", null, VerificationLevel.PHONE_VERIFIED, Set.of("SCOPE_OWNER_TRANSFER_ACCEPT"), Instant.parse("2026-03-02T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new TransferAcceptService(accessPolicy, contextResolver, acceptExecutor);
    }

    @Test
    void accept_delegatesToExecutor() {
        String transferId = "t1";
        AcceptTransferCommand command = new AcceptTransferCommand("nonce-abc", null);
        TransferAcceptContext context = new TransferAcceptContext(false, "NONE", null, null);
        AcceptTransferResult expectedResult = new AcceptTransferResult(
            transferId, "p1", "COMPLETED", "consumer1", Instant.parse("2026-03-01T10:00:00Z"), "outbox1"
        );

        doNothing().when(accessPolicy).assertAcceptAccess(CONSUMER, transferId);
        when(contextResolver.resolveAcceptContext(transferId)).thenReturn(context);
        when(acceptExecutor.accept("consumer1", command, context, transferId)).thenReturn(expectedResult);

        AcceptTransferResult result = service.accept(CONSUMER, transferId, command);

        assertEquals("COMPLETED", result.status());
        assertEquals("consumer1", result.toOwnerId());
        assertEquals("outbox1", result.outboxEventId());
        verify(accessPolicy).assertAcceptAccess(CONSUMER, transferId);
        verify(contextResolver).resolveAcceptContext(transferId);
        verify(acceptExecutor).accept("consumer1", command, context, transferId);
    }

    @Test
    void accept_accessDenied_throws() {
        String transferId = "t1";
        AcceptTransferCommand command = new AcceptTransferCommand("nonce", null);

        doThrow(new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Access denied"))
            .when(accessPolicy).assertAcceptAccess(CONSUMER, transferId);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.accept(CONSUMER, transferId, command)
        );
        assertEquals(WorkflowErrorCode.FORBIDDEN_SCOPE, ex.getErrorCode());
    }

    @Test
    void accept_executorThrows_propagates() {
        String transferId = "t1";
        AcceptTransferCommand command = new AcceptTransferCommand("nonce", null);
        TransferAcceptContext context = new TransferAcceptContext(false, "NONE", null, null);

        doNothing().when(accessPolicy).assertAcceptAccess(CONSUMER, transferId);
        when(contextResolver.resolveAcceptContext(transferId)).thenReturn(context);
        when(acceptExecutor.accept("consumer1", command, context, transferId))
            .thenThrow(new WorkflowDomainException(WorkflowErrorCode.TRANSFER_NOT_FOUND, "Transfer not found"));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.accept(CONSUMER, transferId, command)
        );
        assertEquals(WorkflowErrorCode.TRANSFER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void accept_expired_propagatesFromExecutor() {
        String transferId = "t1";
        AcceptTransferCommand command = new AcceptTransferCommand("nonce1", null);
        TransferAcceptContext context = new TransferAcceptContext(false, "NONE", null, null);

        doNothing().when(accessPolicy).assertAcceptAccess(CONSUMER, transferId);
        when(contextResolver.resolveAcceptContext(transferId)).thenReturn(context);
        when(acceptExecutor.accept("consumer1", command, context, transferId))
            .thenThrow(new WorkflowDomainException(WorkflowErrorCode.TRANSFER_EXPIRED, "Transfer has expired"));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.accept(CONSUMER, transferId, command)
        );
        assertEquals(WorkflowErrorCode.TRANSFER_EXPIRED, ex.getErrorCode());
    }
}
