package io.attestry.workflow.application.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.userauth.domain.auth.model.VerificationLevel;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.transfer.command.TransferCancelService;
import io.attestry.workflow.application.transfer.support.TransferCancelExecutor;
import io.attestry.workflow.application.transfer.policy.TransferAccessPolicy;
import io.attestry.workflow.application.transfer.result.CancelTransferResult;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.transfer.model.AcceptCredential;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.repository.TokenTransferRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferCancelServiceTest {

    @Mock TokenTransferRepository transferRepository;
    @Mock TransferAccessPolicy accessPolicy;
    @Mock TransferCancelExecutor cancelExecutor;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);

    private TransferCancelService service;

    private static final Instant EXPIRES = Instant.parse("2026-03-01T11:00:00Z");
    private static final Instant CREATED = Instant.parse("2026-03-01T09:00:00Z");

    @BeforeEach
    void setUp() {
        service = new TransferCancelService(transferRepository, accessPolicy, cancelExecutor, clock);
    }

    @Test
    void cancel_c2c_byCreator_success() {
        WorkflowActorContext creator = new WorkflowActorContext(
            "token1", "owner1", "tenant1", VerificationLevel.PHONE_VERIFIED, Set.of(), Instant.parse("2026-03-02T00:00:00Z")
        );
        TokenTransfer pending = TokenTransfer.createC2C(
            "t1", "p1", "owner1",
            AcceptCredential.ofQr("nonce1"),
            EXPIRES, CREATED, "owner1"
        );

        when(transferRepository.findById("t1")).thenReturn(Optional.of(pending));
        doNothing().when(accessPolicy).assertCancelAccess(creator, "t1", pending);
        when(cancelExecutor.cancel(pending, "owner1", Instant.parse("2026-03-01T10:00:00Z")))
            .thenReturn(new CancelTransferResult("t1", "p1", "CANCELLED", Instant.parse("2026-03-01T10:00:00Z")));

        CancelTransferResult result = service.cancel(creator, "t1");

        assertEquals("CANCELLED", result.status());
        assertEquals("p1", result.passportId());
        assertNotNull(result.cancelledAt());
    }

    @Test
    void cancel_c2c_byNonCreator_throws() {
        WorkflowActorContext other = new WorkflowActorContext(
            "token2", "otherUser", "tenant1", VerificationLevel.PHONE_VERIFIED, Set.of(), Instant.parse("2026-03-02T00:00:00Z")
        );
        TokenTransfer pending = TokenTransfer.createC2C(
            "t1", "p1", "owner1",
            AcceptCredential.ofQr("nonce1"),
            EXPIRES, CREATED, "owner1"
        );

        when(transferRepository.findById("t1")).thenReturn(Optional.of(pending));
        org.mockito.Mockito.doThrow(new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Only the transfer creator can cancel a C2C transfer"))
            .when(accessPolicy).assertCancelAccess(other, "t1", pending);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.cancel(other, "t1")
        );
        assertEquals(WorkflowErrorCode.FORBIDDEN_SCOPE, ex.getErrorCode());
    }

    @Test
    void cancel_b2c_withTenantAuth_success() {
        WorkflowActorContext retailPrincipal = new WorkflowActorContext(
            "token3", "retailUser", "tenant1", VerificationLevel.PHONE_VERIFIED, Set.of(), Instant.parse("2026-03-02T00:00:00Z")
        );
        TokenTransfer pending = TokenTransfer.createB2C(
            "t1", "p1", "tenant1",
            AcceptCredential.ofQr("nonce1"),
            EXPIRES, CREATED, "retailUser"
        );

        when(transferRepository.findById("t1")).thenReturn(Optional.of(pending));
        doNothing().when(accessPolicy).assertCancelAccess(retailPrincipal, "t1", pending);
        when(cancelExecutor.cancel(pending, "retailUser", Instant.parse("2026-03-01T10:00:00Z")))
            .thenReturn(new CancelTransferResult("t1", "p1", "CANCELLED", Instant.parse("2026-03-01T10:00:00Z")));

        CancelTransferResult result = service.cancel(retailPrincipal, "t1");

        assertEquals("CANCELLED", result.status());
        verify(accessPolicy).assertCancelAccess(retailPrincipal, "t1", pending);
    }

    @Test
    void cancel_notFound_throws() {
        WorkflowActorContext principal = new WorkflowActorContext(
            "token1", "user1", "tenant1", VerificationLevel.PHONE_VERIFIED, Set.of(), Instant.parse("2026-03-02T00:00:00Z")
        );

        when(transferRepository.findById("missing")).thenReturn(Optional.empty());

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.cancel(principal, "missing")
        );
        assertEquals(WorkflowErrorCode.TRANSFER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void cancel_alreadyCompleted_throws() {
        WorkflowActorContext creator = new WorkflowActorContext(
            "token1", "owner1", "tenant1", VerificationLevel.PHONE_VERIFIED, Set.of(), Instant.parse("2026-03-02T00:00:00Z")
        );
        TokenTransfer completed = TokenTransfer.createC2C(
            "t1", "p1", "owner1",
            AcceptCredential.ofQr("nonce1"),
            EXPIRES, CREATED, "owner1"
        ).complete("consumer1", Instant.parse("2026-03-01T09:30:00Z"));

        when(transferRepository.findById("t1")).thenReturn(Optional.of(completed));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.cancel(creator, "t1")
        );
        assertEquals(WorkflowErrorCode.TRANSFER_INVALID_STATE, ex.getErrorCode());
    }
}
