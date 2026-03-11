package io.attestry.workflow.application.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.userauth.domain.identity.model.VerificationLevel;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.transfer.TransferProductReadPort;
import io.attestry.workflow.application.transfer.command.CreateB2CTransferCommand;
import io.attestry.workflow.application.transfer.command.CreateC2CTransferCommand;
import io.attestry.workflow.application.transfer.policy.TransferAccessPolicy;
import io.attestry.workflow.application.transfer.result.CreateTransferResult;
import io.attestry.workflow.application.transfer.support.TransferContextResolver;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.transfer.model.AcceptMethod;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferType;
import io.attestry.workflow.domain.transfer.model.TransferStatus;
import io.attestry.workflow.domain.transfer.policy.TransferCreatePolicy.TransferCreateContext;
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
    @Mock TransferAccessPolicy accessPolicy;
    @Mock TransferContextResolver contextResolver;
    @Mock TransferCreateExecutor createExecutor;

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
            transferRepository, productReadPort, accessPolicy,
            contextResolver, createExecutor, clock
        );
    }

    @Test
    void createC2C_delegatesToExecutor() {
        CreateC2CTransferCommand command = new CreateC2CTransferCommand(AcceptMethod.QR, null, EXPIRES);
        TransferCreateContext context = new TransferCreateContext(
            "user1", null, "ACTIVE", "NONE", "tenant1", "user1", false, false
        );
        CreateTransferResult expectedResult = new CreateTransferResult(
            "t1", "p1", "C2C", "PENDING", "QR", "nonce-1", EXPIRES
        );

        doNothing().when(accessPolicy).assertCreateC2CAccess(OWNER_PRINCIPAL, "p1");
        when(contextResolver.resolveCreateContext("user1", null, "p1")).thenReturn(context);
        when(createExecutor.createC2C("p1", "user1", command, context)).thenReturn(expectedResult);

        CreateTransferResult result = service.createC2C(OWNER_PRINCIPAL, "p1", command);

        assertEquals("C2C", result.transferType());
        assertEquals("PENDING", result.status());
        assertEquals("QR", result.acceptMethod());
        verify(accessPolicy).assertCreateC2CAccess(OWNER_PRINCIPAL, "p1");
        verify(contextResolver).resolveCreateContext("user1", null, "p1");
        verify(createExecutor).createC2C("p1", "user1", command, context);
    }

    @Test
    void createC2C_code_delegatesToExecutor() {
        CreateC2CTransferCommand command = new CreateC2CTransferCommand(AcceptMethod.CODE, "mypassword", EXPIRES);
        TransferCreateContext context = new TransferCreateContext(
            "user1", null, "ACTIVE", "NONE", "tenant1", "user1", false, false
        );
        CreateTransferResult expectedResult = new CreateTransferResult(
            "t1", "p1", "C2C", "PENDING", "CODE", null, EXPIRES
        );

        doNothing().when(accessPolicy).assertCreateC2CAccess(OWNER_PRINCIPAL, "p1");
        when(contextResolver.resolveCreateContext("user1", null, "p1")).thenReturn(context);
        when(createExecutor.createC2C("p1", "user1", command, context)).thenReturn(expectedResult);

        CreateTransferResult result = service.createC2C(OWNER_PRINCIPAL, "p1", command);

        assertEquals("CODE", result.acceptMethod());
    }

    @Test
    void createC2C_accessDenied_throws() {
        CreateC2CTransferCommand command = new CreateC2CTransferCommand(AcceptMethod.QR, null, EXPIRES);

        doThrow(new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Access denied"))
            .when(accessPolicy).assertCreateC2CAccess(OWNER_PRINCIPAL, "p1");

        assertThrows(WorkflowDomainException.class, () ->
            service.createC2C(OWNER_PRINCIPAL, "p1", command)
        );
    }

    @Test
    void createC2C_executorThrows_propagates() {
        CreateC2CTransferCommand command = new CreateC2CTransferCommand(AcceptMethod.QR, null, EXPIRES);
        TransferCreateContext context = new TransferCreateContext(
            "user1", null, "ACTIVE", "NONE", "tenant1", "user1", false, true
        );

        doNothing().when(accessPolicy).assertCreateC2CAccess(OWNER_PRINCIPAL, "p1");
        when(contextResolver.resolveCreateContext("user1", null, "p1")).thenReturn(context);
        when(createExecutor.createC2C("p1", "user1", command, context))
            .thenThrow(new WorkflowDomainException(WorkflowErrorCode.TRANSFER_ALREADY_PENDING, "A pending transfer already exists"));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.createC2C(OWNER_PRINCIPAL, "p1", command)
        );
        assertEquals("TRANSFER_ALREADY_PENDING", ex.getErrorCode().getCode());
    }

    @Test
    void createB2C_delegatesToExecutor() {
        CreateB2CTransferCommand command = new CreateB2CTransferCommand(AcceptMethod.QR, null, EXPIRES);
        TransferCreateContext context = new TransferCreateContext(
            "retailUser", "tenant1", "ACTIVE", "NONE", "brandTenant", null, true, false
        );
        CreateTransferResult expectedResult = new CreateTransferResult(
            "t1", "p1", "B2C", "PENDING", "QR", "nonce-1", EXPIRES
        );

        doNothing().when(accessPolicy).assertCreateB2CAccess(RETAIL_PRINCIPAL, "tenant1", "p1");
        when(contextResolver.resolveCreateContext("retailUser", "tenant1", "p1")).thenReturn(context);
        when(createExecutor.createB2C("p1", "tenant1", "retailUser", command, context)).thenReturn(expectedResult);

        CreateTransferResult result = service.createB2C(RETAIL_PRINCIPAL, "tenant1", "p1", command);

        assertEquals("B2C", result.transferType());
        assertEquals("PENDING", result.status());
        verify(accessPolicy).assertCreateB2CAccess(RETAIL_PRINCIPAL, "tenant1", "p1");
        verify(contextResolver).resolveCreateContext("retailUser", "tenant1", "p1");
        verify(createExecutor).createB2C("p1", "tenant1", "retailUser", command, context);
    }

    @Test
    void createB2C_accessDenied_throws() {
        CreateB2CTransferCommand command = new CreateB2CTransferCommand(AcceptMethod.QR, null, EXPIRES);

        doThrow(new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Access denied"))
            .when(accessPolicy).assertCreateB2CAccess(RETAIL_PRINCIPAL, "tenant1", "p1");

        assertThrows(WorkflowDomainException.class, () ->
            service.createB2C(RETAIL_PRINCIPAL, "tenant1", "p1", command)
        );
    }

    @Test
    void findLatestActivePendingB2CByPassportId_returnsRetailPendingTransfer() {
        doNothing().when(accessPolicy).assertFindPendingB2CAccess(RETAIL_PRINCIPAL, "tenant1", "p1");
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
