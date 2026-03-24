package io.attestry.workflow.application.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.workflow.application.port.transfer.TransferProductReadPort;
import io.attestry.workflow.application.port.transfer.TransferProductReadPort.TransferPassportState;
import io.attestry.workflow.application.transfer.support.TransferContextResolver;
import io.attestry.workflow.domain.WorkflowDomainException;

import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.transfer.model.AcceptCredential;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.policy.TransferAcceptPolicy.TransferAcceptContext;
import io.attestry.workflow.domain.transfer.policy.TransferCreatePolicy.TransferCreateContext;
import io.attestry.workflow.domain.transfer.repository.TokenTransferRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferContextResolverTest {

    private static final Instant NOW = Instant.parse("2026-03-12T10:00:00Z");

    @Mock TransferProductReadPort productReadPort;
    @Mock TokenTransferRepository transferRepository;

    private TransferContextResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TransferContextResolver(productReadPort, transferRepository);
    }

    @Test
    void resolveCreateContext_forB2c_combinesProjectionAndRepositoryInputs() {
        when(productReadPort.findPassportState("p1")).thenReturn(Optional.of(
            new TransferPassportState("p1", "brand-1", "ACTIVE", "NONE")
        ));
        when(productReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.empty());
        when(transferRepository.existsActivePendingByPassportId("p1")).thenReturn(false);
        when(productReadPort.hasRetailPermission("p1", "retail-1")).thenReturn(true);

        TransferCreateContext context = resolver.resolveCreateContext("retail-user", "retail-1", "p1");

        assertEquals("retail-user", context.actorUserId());
        assertEquals("retail-1", context.requestTenantId());
        assertEquals("brand-1", context.passportTenantId());
        assertNull(context.currentOwnerId());
        assertTrue(context.hasRetailPermission());
        assertFalse(context.pendingTransferExists());
    }

    @Test
    void resolveCreateContext_forC2c_doesNotQueryRetailPermission() {
        when(productReadPort.findPassportState("p2")).thenReturn(Optional.of(
            new TransferPassportState("p2", "brand-1", "ACTIVE", "NONE")
        ));
        when(productReadPort.findCurrentOwnerId("p2")).thenReturn(Optional.of("owner-1"));
        when(transferRepository.existsActivePendingByPassportId("p2")).thenReturn(true);

        TransferCreateContext context = resolver.resolveCreateContext("owner-1", null, "p2");

        assertEquals("owner-1", context.actorUserId());
        assertNull(context.requestTenantId());
        assertEquals("owner-1", context.currentOwnerId());
        assertFalse(context.hasRetailPermission());
        assertTrue(context.pendingTransferExists());
        verify(productReadPort, never()).hasRetailPermission("p2", null);
    }

    @Test
    void resolveAcceptContext_forC2c_loadsCurrentOwner() {
        TokenTransfer transfer = TokenTransfer.createC2C(
            "t1",
            "p1",
            "owner-1",
            AcceptCredential.ofQr("nonce-1"),
            NOW.plusSeconds(3600),
            NOW.minusSeconds(60),
            "owner-1"
        );
        when(productReadPort.findPassportState("p1")).thenReturn(Optional.of(
            new TransferPassportState("p1", "brand-1", "ACTIVE", "NONE")
        ));
        when(productReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("owner-1"));

        TransferAcceptContext context = resolver.resolveAcceptContext(transfer);

        assertTrue(context.isC2C());
        assertEquals("owner-1", context.currentOwnerId());
        assertEquals("owner-1", context.expectedFromOwnerId());
        verify(productReadPort).findCurrentOwnerId("p1");
    }

    @Test
    void resolveAcceptContext_forB2cSkipsCurrentOwnerLookup() {
        TokenTransfer transfer = TokenTransfer.createB2C(
            "t2",
            "p2",
            "retail-1",
            AcceptCredential.ofQr("nonce-2"),
            NOW.plusSeconds(3600),
            NOW.minusSeconds(60),
            "retail-user-1"
        );
        when(productReadPort.findPassportState("p2")).thenReturn(Optional.of(
            new TransferPassportState("p2", "brand-1", "ACTIVE", "NONE")
        ));

        TransferAcceptContext context = resolver.resolveAcceptContext(transfer);

        assertFalse(context.isC2C());
        assertNull(context.currentOwnerId());
        assertNull(context.expectedFromOwnerId());
        verify(productReadPort, never()).findCurrentOwnerId("p2");
    }

    @Test
    void resolveAcceptContext_whenPassportMissing_throws() {
        TokenTransfer transfer = TokenTransfer.createB2C(
            "t3",
            "missing-passport",
            "retail-1",
            AcceptCredential.ofQr("nonce-3"),
            NOW.plusSeconds(3600),
            NOW.minusSeconds(60),
            "retail-user-1"
        );
        when(productReadPort.findPassportState("missing-passport")).thenReturn(Optional.empty());

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            resolver.resolveAcceptContext(transfer)
        );

        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }
}
