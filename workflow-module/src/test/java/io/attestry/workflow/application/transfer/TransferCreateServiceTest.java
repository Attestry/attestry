package io.attestry.workflow.application.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.userauth.domain.identity.model.VerificationLevel;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.TransferProductReadPort;
import io.attestry.workflow.application.port.TransferProductReadPort.TransferPassportState;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.transfer.command.CreateB2CTransferCommand;
import io.attestry.workflow.application.transfer.command.CreateC2CTransferCommand;
import io.attestry.workflow.application.transfer.result.CreateTransferResult;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.transfer.model.AcceptMethod;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferType;
import io.attestry.workflow.domain.transfer.model.TransferStatus;
import io.attestry.workflow.domain.transfer.policy.TransferCreatePolicy;
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
class TransferCreateServiceTest {

    @Mock TokenTransferRepository transferRepository;
    @Mock TransferProductReadPort productReadPort;
    @Mock WorkflowAuthorizationSupport authorizationSupport;

    private final TransferHashSupport hashSupport = new TransferHashSupport();
    private final TransferCreatePolicy createPolicy = new TransferCreatePolicy();
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);

    private TransferCreateService service;

    private static final Instant EXPIRES = Instant.parse("2026-03-01T11:00:00Z");
    private static final AuthPrincipal OWNER_PRINCIPAL = new AuthPrincipal(
        "token1", "user1", "tenant1", VerificationLevel.PHONE_VERIFIED, Set.of("SCOPE_OWNER_TRANSFER_CREATE"), Instant.parse("2026-03-02T00:00:00Z")
    );
    private static final AuthPrincipal RETAIL_PRINCIPAL = new AuthPrincipal(
        "token2", "retailUser", "tenant1", VerificationLevel.PHONE_VERIFIED, Set.of("SCOPE_RETAIL_TRANSFER_CREATE"), Instant.parse("2026-03-02T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new TransferCreateService(
            transferRepository, productReadPort, authorizationSupport,
            hashSupport, createPolicy, clock
        );
    }

    @Test
    void createC2C_qr_success() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(productReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new TransferPassportState("p1", "tenant1", "ACTIVE", "NONE")));
        when(productReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("user1"));
        when(transferRepository.existsActivePendingByPassportId("p1")).thenReturn(false);
        when(transferRepository.save(any(TokenTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTransferResult result = service.createC2C(
            OWNER_PRINCIPAL, "p1",
            new CreateC2CTransferCommand(AcceptMethod.QR, null, EXPIRES)
        );

        assertEquals("C2C", result.transferType());
        assertEquals("PENDING", result.status());
        assertEquals("QR", result.acceptMethod());
        assertNotNull(result.qrNonce());
        assertNotNull(result.transferId());
        verify(transferRepository).save(any(TokenTransfer.class));
    }

    @Test
    void createC2C_code_success() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(productReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new TransferPassportState("p1", "tenant1", "ACTIVE", "NONE")));
        when(productReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("user1"));
        when(transferRepository.existsActivePendingByPassportId("p1")).thenReturn(false);
        when(transferRepository.save(any(TokenTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTransferResult result = service.createC2C(
            OWNER_PRINCIPAL, "p1",
            new CreateC2CTransferCommand(AcceptMethod.CODE, "mypassword", EXPIRES)
        );

        assertEquals("CODE", result.acceptMethod());
        assertNull(result.qrNonce());
    }

    @Test
    void createC2C_notOwner_throws() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(productReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new TransferPassportState("p1", "tenant1", "ACTIVE", "NONE")));
        when(productReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("otherUser"));
        when(transferRepository.existsActivePendingByPassportId("p1")).thenReturn(false);

        assertThrows(WorkflowDomainException.class, () ->
            service.createC2C(OWNER_PRINCIPAL, "p1", new CreateC2CTransferCommand(AcceptMethod.QR, null, EXPIRES))
        );
    }

    @Test
    void createC2C_alreadyPending_throws() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(productReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new TransferPassportState("p1", "tenant1", "ACTIVE", "NONE")));
        when(productReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("user1"));
        when(transferRepository.existsActivePendingByPassportId("p1")).thenReturn(true);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.createC2C(OWNER_PRINCIPAL, "p1", new CreateC2CTransferCommand(AcceptMethod.QR, null, EXPIRES))
        );
        assertEquals("TRANSFER_ALREADY_PENDING", ex.getErrorCode().getCode());
    }

    @Test
    void createC2C_riskFlagged_throws() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(productReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new TransferPassportState("p1", "tenant1", "ACTIVE", "HIGH")));
        when(productReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.empty());
        when(transferRepository.existsActivePendingByPassportId("p1")).thenReturn(false);

        assertThrows(WorkflowDomainException.class, () ->
            service.createC2C(OWNER_PRINCIPAL, "p1", new CreateC2CTransferCommand(AcceptMethod.QR, null, EXPIRES))
        );
    }

    @Test
    void createB2C_qr_success() {
        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(productReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new TransferPassportState("p1", "brandTenant", "ACTIVE", "NONE")));
        when(productReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.empty());
        when(productReadPort.hasRetailPermission("p1", "tenant1")).thenReturn(true);
        when(transferRepository.existsActivePendingByPassportId("p1")).thenReturn(false);
        when(transferRepository.save(any(TokenTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTransferResult result = service.createB2C(
            RETAIL_PRINCIPAL, "tenant1", "p1",
            new CreateB2CTransferCommand(AcceptMethod.QR, null, EXPIRES)
        );

        assertEquals("B2C", result.transferType());
        assertEquals("PENDING", result.status());
        assertNotNull(result.qrNonce());
    }

    @Test
    void createB2C_alreadyHasOwner_throws() {
        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(productReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new TransferPassportState("p1", "tenant1", "ACTIVE", "NONE")));
        when(productReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("existingOwner"));
        when(transferRepository.existsActivePendingByPassportId("p1")).thenReturn(false);

        assertThrows(WorkflowDomainException.class, () ->
            service.createB2C(RETAIL_PRINCIPAL, "tenant1", "p1", new CreateB2CTransferCommand(AcceptMethod.QR, null, EXPIRES))
        );
    }

    @Test
    void createB2C_noRetailPermission_throws() {
        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(productReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new TransferPassportState("p1", "tenant1", "ACTIVE", "NONE")));
        when(productReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.empty());
        when(productReadPort.hasRetailPermission("p1", "tenant1")).thenReturn(false);
        when(transferRepository.existsActivePendingByPassportId("p1")).thenReturn(false);

        assertThrows(WorkflowDomainException.class, () ->
            service.createB2C(RETAIL_PRINCIPAL, "tenant1", "p1", new CreateB2CTransferCommand(AcceptMethod.QR, null, EXPIRES))
        );
    }

    @Test
    void findLatestActivePendingB2CByPassportId_returnsRetailPendingTransfer() {
        doNothing().when(authorizationSupport).assertTenantContext(RETAIL_PRINCIPAL, "tenant1");
        doNothing().when(authorizationSupport).assertLivePermission(RETAIL_PRINCIPAL, "tenant1", "RETAIL_TRANSFER_CREATE",
            "transfer:pending:p1");
        when(productReadPort.hasRetailPermission("p1", "tenant1")).thenReturn(true);
        when(transferRepository.findLatestActivePendingByPassportId("p1", Instant.parse("2026-03-01T10:00:00Z")))
            .thenReturn(Optional.of(new TokenTransfer(
                "t1",
                "p1",
                TransferType.B2C,
                TransferStatus.PENDING,
                AcceptMethod.QR,
                null,
                null,
                "tenant1",
                "nonce-1",
                null,
                null,
                0,
                EXPIRES,
                Instant.parse("2026-03-01T10:00:00Z"),
                "retailUser",
                null,
                null,
                null
            )));

        Optional<CreateTransferResult> result = service.findLatestActivePendingB2CByPassportId(RETAIL_PRINCIPAL, "tenant1", "p1");

        assertTrue(result.isPresent());
        assertEquals("t1", result.get().transferId());
        assertEquals("B2C", result.get().transferType());
    }
}
