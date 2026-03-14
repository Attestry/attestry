package io.attestry.workflow.domain.transfer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.workflow.domain.WorkflowDomainException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TokenTransferTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant EXPIRES = Instant.parse("2026-03-01T11:00:00Z");
    private static final Instant AFTER_EXPIRY = Instant.parse("2026-03-01T12:00:00Z");
    private static final AcceptCredential QR_CREDENTIAL = AcceptCredential.ofQr("nonce1");
    private static final AcceptCredential CODE_CREDENTIAL = AcceptCredential.ofCode("hash1", "salt1");

    @Test
    void createC2C_setsFieldsCorrectly() {
        TokenTransfer transfer = TokenTransfer.createC2C(
            "t1", "p1", "owner1", QR_CREDENTIAL, EXPIRES, NOW, "user1"
        );

        assertEquals("t1", transfer.transferId());
        assertEquals("p1", transfer.passportId());
        assertEquals(TransferType.C2C, transfer.transferType());
        assertEquals(TransferStatus.PENDING, transfer.status());
        assertEquals(AcceptMethod.QR, transfer.acceptMethod());
        assertEquals("owner1", transfer.fromOwnerId());
        assertNull(transfer.toOwnerId());
        assertNull(transfer.tenantId());
        assertEquals("nonce1", transfer.qrNonce());
        assertEquals(0, transfer.attemptCount());
    }

    @Test
    void createB2C_setsFieldsCorrectly() {
        TokenTransfer transfer = TokenTransfer.createB2C(
            "t1", "p1", "tenant1", CODE_CREDENTIAL, EXPIRES, NOW, "user1"
        );

        assertEquals(TransferType.B2C, transfer.transferType());
        assertEquals(TransferStatus.PENDING, transfer.status());
        assertNull(transfer.fromOwnerId());
        assertEquals("tenant1", transfer.tenantId());
        assertEquals("hash1", transfer.codeHash());
        assertEquals("salt1", transfer.codeSalt());
    }

    @Test
    void createC2C_requiresFromOwnerId() {
        assertThrows(WorkflowDomainException.class, () ->
            TokenTransfer.createC2C(
                "t1", "p1", null, QR_CREDENTIAL, EXPIRES, NOW, "user1"
            )
        );
    }

    @Test
    void createB2C_requiresTenantId() {
        assertThrows(WorkflowDomainException.class, () ->
            TokenTransfer.createB2C(
                "t1", "p1", null, QR_CREDENTIAL, EXPIRES, NOW, "user1"
            )
        );
    }

    @Test
    void complete_fromPending_succeeds() {
        TokenTransfer transfer = createPendingC2C();

        TokenTransfer completed = transfer.complete("newOwner", NOW);

        assertEquals(TransferStatus.COMPLETED, completed.status());
        assertEquals("newOwner", completed.toOwnerId());
        assertNotNull(completed.completedAt());
    }

    @Test
    void complete_whenExpired_throws() {
        TokenTransfer transfer = createPendingC2C();

        assertThrows(WorkflowDomainException.class, () ->
            transfer.complete("newOwner", AFTER_EXPIRY)
        );
    }

    @Test
    void complete_whenNotPending_throws() {
        TokenTransfer transfer = createPendingC2C().complete("newOwner", NOW);

        assertThrows(WorkflowDomainException.class, () ->
            transfer.complete("anotherOwner", NOW)
        );
    }

    @Test
    void cancel_fromPending_succeeds() {
        TokenTransfer transfer = createPendingC2C();

        TokenTransfer cancelled = transfer.cancel("user1", NOW);

        assertEquals(TransferStatus.CANCELLED, cancelled.status());
        assertEquals("user1", cancelled.cancelledByUserId());
        assertNotNull(cancelled.cancelledAt());
    }

    @Test
    void cancel_whenNotPending_throws() {
        TokenTransfer transfer = createPendingC2C().cancel("user1", NOW);

        assertThrows(WorkflowDomainException.class, () ->
            transfer.cancel("user1", NOW)
        );
    }

    @Test
    void markExpired_fromPending_succeeds() {
        TokenTransfer transfer = createPendingC2C();

        TokenTransfer expired = transfer.markExpired(AFTER_EXPIRY);

        assertEquals(TransferStatus.EXPIRED, expired.status());
    }

    @Test
    void markExpired_whenNotPending_throws() {
        TokenTransfer transfer = createPendingC2C().complete("newOwner", NOW);

        assertThrows(WorkflowDomainException.class, () ->
            transfer.markExpired(AFTER_EXPIRY)
        );
    }

    @Test
    void isExpired_returnsCorrectly() {
        TokenTransfer transfer = createPendingC2C();

        assertFalse(transfer.isExpired(NOW));
        assertTrue(transfer.isExpired(AFTER_EXPIRY));
    }

    @Test
    void incrementAttempt_increasesCount() {
        TokenTransfer transfer = createPendingC2C();

        assertEquals(0, transfer.attemptCount());

        transfer = transfer.incrementAttempt();
        assertEquals(1, transfer.attemptCount());

        transfer = transfer.incrementAttempt();
        assertEquals(2, transfer.attemptCount());
    }

    @Test
    void isBruteForceBlocked_afterMaxAttempts() {
        TokenTransfer transfer = createPendingC2C();

        for (int i = 0; i < 4; i++) {
            transfer = transfer.incrementAttempt();
            assertFalse(transfer.isBruteForceBlocked());
        }

        transfer = transfer.incrementAttempt();
        assertTrue(transfer.isBruteForceBlocked());
    }

    @Test
    void verifyQrNonce_matchesCorrectly() {
        AcceptCredential credential = AcceptCredential.ofQr("nonce-abc");
        TokenTransfer transfer = TokenTransfer.createC2C(
            "t1", "p1", "owner1", credential, EXPIRES, NOW, "user1"
        );

        assertTrue(transfer.verifyQrNonce("nonce-abc"));
        assertFalse(transfer.verifyQrNonce("wrong-nonce"));
    }

    private TokenTransfer createPendingC2C() {
        return TokenTransfer.createC2C(
            "t1", "p1", "owner1", QR_CREDENTIAL, EXPIRES, NOW, "user1"
        );
    }
}
