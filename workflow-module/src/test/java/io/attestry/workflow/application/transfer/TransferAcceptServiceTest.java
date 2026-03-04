package io.attestry.workflow.application.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.userauth.domain.identity.model.VerificationLevel;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.TransferLedgerOutboxPort;
import io.attestry.workflow.application.port.TransferOwnershipUpdatePort;
import io.attestry.workflow.application.port.TransferProductReadPort;
import io.attestry.workflow.application.port.TransferProductReadPort.TransferPassportState;
import io.attestry.workflow.application.shipment.result.WorkflowLedgerEventEnvelope;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.transfer.command.AcceptTransferCommand;
import io.attestry.workflow.application.transfer.result.AcceptTransferResult;
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
class TransferAcceptServiceTest {

    @Mock TokenTransferRepository transferRepository;
    @Mock TransferProductReadPort productReadPort;
    @Mock TransferOwnershipUpdatePort ownershipUpdatePort;
    @Mock TransferLedgerOutboxPort outboxPort;
    @Mock WorkflowAuthorizationSupport authorizationSupport;

    private final TransferHashSupport hashSupport = new TransferHashSupport();
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);

    private TransferAcceptService service;

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant EXPIRES = Instant.parse("2026-03-01T11:00:00Z");
    private static final Instant CREATED = Instant.parse("2026-03-01T09:00:00Z");
    private static final AuthPrincipal CONSUMER = new AuthPrincipal(
        "token1", "consumer1", null, null, VerificationLevel.PHONE_VERIFIED, Set.of("SCOPE_OWNER_TRANSFER_ACCEPT"), Instant.parse("2026-03-02T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new TransferAcceptService(
            transferRepository, productReadPort, ownershipUpdatePort,
            outboxPort, authorizationSupport, hashSupport, clock
        );
    }

    @Test
    void accept_qr_b2c_success() {
        String nonce = "nonce-abc";
        TokenTransfer pending = TokenTransfer.createB2C(
            "t1", "p1", "tenant1", "group1",
            AcceptCredential.ofQr(nonce),
            EXPIRES, CREATED, "retailUser"
        );

        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(transferRepository.findById("t1")).thenReturn(Optional.of(pending));
        when(productReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new TransferPassportState("p1", "tenant1", "group1", "ACTIVE", "NONE")));
        when(transferRepository.save(any(TokenTransfer.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(ownershipUpdatePort).upsertOwner(anyString(), anyString(), any(Instant.class));
        when(outboxPort.enqueue(any(WorkflowLedgerEventEnvelope.class))).thenReturn("outbox1");

        AcceptTransferResult result = service.accept(CONSUMER, "t1", new AcceptTransferCommand(nonce, null));

        assertEquals("COMPLETED", result.status());
        assertEquals("consumer1", result.toOwnerId());
        assertNotNull(result.completedAt());
        assertEquals("outbox1", result.outboxEventId());
        verify(ownershipUpdatePort).upsertOwner("p1", "consumer1", NOW);
    }

    @Test
    void accept_code_c2c_success() {
        String salt = hashSupport.generateSalt();
        String hash = hashSupport.hash("secret123", salt);
        TokenTransfer pending = TokenTransfer.createC2C(
            "t1", "p1", "owner1",
            AcceptCredential.ofCode(hash, salt),
            EXPIRES, CREATED, "owner1"
        );

        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(transferRepository.findById("t1")).thenReturn(Optional.of(pending));
        when(productReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new TransferPassportState("p1", "tenant1", "group1", "ACTIVE", "NONE")));
        when(productReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("owner1"));
        when(transferRepository.save(any(TokenTransfer.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(ownershipUpdatePort).upsertOwner(anyString(), anyString(), any(Instant.class));
        when(outboxPort.enqueue(any(WorkflowLedgerEventEnvelope.class))).thenReturn("outbox1");

        AcceptTransferResult result = service.accept(CONSUMER, "t1", new AcceptTransferCommand(null, "secret123"));

        assertEquals("COMPLETED", result.status());
        assertEquals("consumer1", result.toOwnerId());
    }

    @Test
    void accept_notFound_throws() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(transferRepository.findById("missing")).thenReturn(Optional.empty());

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.accept(CONSUMER, "missing", new AcceptTransferCommand("nonce", null))
        );
        assertEquals(WorkflowErrorCode.TRANSFER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void accept_expired_throws() {
        Clock expiredClock = Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);
        TransferAcceptService expiredService = new TransferAcceptService(
            transferRepository, productReadPort, ownershipUpdatePort,
            outboxPort, authorizationSupport, hashSupport, expiredClock
        );

        TokenTransfer pending = TokenTransfer.createB2C(
            "t1", "p1", "tenant1", "group1",
            AcceptCredential.ofQr("nonce1"),
            EXPIRES, CREATED, "retailUser"
        );

        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(transferRepository.findById("t1")).thenReturn(Optional.of(pending));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            expiredService.accept(CONSUMER, "t1", new AcceptTransferCommand("nonce1", null))
        );
        assertEquals(WorkflowErrorCode.TRANSFER_EXPIRED, ex.getErrorCode());
    }

    @Test
    void accept_wrongQrNonce_throws() {
        TokenTransfer pending = TokenTransfer.createB2C(
            "t1", "p1", "tenant1", "group1",
            AcceptCredential.ofQr("nonce-correct"),
            EXPIRES, CREATED, "retailUser"
        );

        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(transferRepository.findById("t1")).thenReturn(Optional.of(pending));
        when(productReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new TransferPassportState("p1", "tenant1", "group1", "ACTIVE", "NONE")));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.accept(CONSUMER, "t1", new AcceptTransferCommand("wrong-nonce", null))
        );
        assertEquals(WorkflowErrorCode.TRANSFER_NONCE_MISMATCH, ex.getErrorCode());
    }

    @Test
    void accept_wrongCode_throws() {
        String salt = hashSupport.generateSalt();
        String hash = hashSupport.hash("correct", salt);
        TokenTransfer pending = TokenTransfer.createC2C(
            "t1", "p1", "owner1",
            AcceptCredential.ofCode(hash, salt),
            EXPIRES, CREATED, "owner1"
        );

        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(transferRepository.findById("t1")).thenReturn(Optional.of(pending));
        when(productReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new TransferPassportState("p1", "tenant1", "group1", "ACTIVE", "NONE")));
        when(productReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("owner1"));
        when(transferRepository.save(any(TokenTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.accept(CONSUMER, "t1", new AcceptTransferCommand(null, "wrong"))
        );
        assertEquals(WorkflowErrorCode.TRANSFER_CODE_MISMATCH, ex.getErrorCode());
        verify(ownershipUpdatePort, never()).upsertOwner(anyString(), anyString(), any());
    }

    @Test
    void accept_bruteForceBlocked_autoCancels() {
        String salt = hashSupport.generateSalt();
        String hash = hashSupport.hash("correct", salt);
        TokenTransfer pending = TokenTransfer.createC2C(
            "t1", "p1", "owner1",
            AcceptCredential.ofCode(hash, salt),
            EXPIRES, CREATED, "owner1"
        );
        // Simulate 4 prior attempts
        for (int i = 0; i < 4; i++) {
            pending = pending.incrementAttempt();
        }

        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(transferRepository.findById("t1")).thenReturn(Optional.of(pending));
        when(productReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new TransferPassportState("p1", "tenant1", "group1", "ACTIVE", "NONE")));
        when(productReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("owner1"));
        when(transferRepository.save(any(TokenTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.accept(CONSUMER, "t1", new AcceptTransferCommand(null, "wrong"))
        );
        assertEquals(WorkflowErrorCode.TRANSFER_CODE_MISMATCH, ex.getErrorCode());
    }

    @Test
    void accept_c2c_ownershipChanged_throws() {
        TokenTransfer pending = TokenTransfer.createC2C(
            "t1", "p1", "owner1",
            AcceptCredential.ofQr("nonce1"),
            EXPIRES, CREATED, "owner1"
        );

        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(transferRepository.findById("t1")).thenReturn(Optional.of(pending));
        when(productReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new TransferPassportState("p1", "tenant1", "group1", "ACTIVE", "NONE")));
        when(productReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("differentOwner"));

        assertThrows(WorkflowDomainException.class, () ->
            service.accept(CONSUMER, "t1", new AcceptTransferCommand("nonce1", null))
        );
    }
}
