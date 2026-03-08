package io.attestry.workflow.application.claim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.product.application.usecase.ProductMintUseCase;
import io.attestry.product.application.usecase.ProductMintUseCase.MintProductCommand;
import io.attestry.product.application.usecase.ProductMintUseCase.MintedProductResult;
import io.attestry.userauth.application.port.ObjectStoragePort;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.domain.identity.model.VerificationLevel;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.claim.command.ApprovePurchaseClaimCommand;
import io.attestry.workflow.application.claim.result.ApprovePurchaseClaimResult;
import io.attestry.workflow.application.claim.result.RejectPurchaseClaimResult;
import io.attestry.workflow.application.port.WorkflowEvidencePort;
import io.attestry.workflow.application.port.WorkflowLedgerOutboxPort;
import io.attestry.workflow.application.port.TransferOwnershipUpdatePort;
import io.attestry.workflow.application.shipment.result.WorkflowLedgerEventEnvelope;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import io.attestry.workflow.domain.claim.model.PurchaseClaimStatus;
import io.attestry.workflow.domain.claim.repository.PurchaseClaimRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseClaimAdminServiceTest {

    @Mock PurchaseClaimRepository purchaseClaimRepository;
    @Mock ProductMintUseCase productMintUseCase;
    @Mock TransferOwnershipUpdatePort ownershipUpdatePort;
    @Mock WorkflowLedgerOutboxPort ledgerOutboxPort;
    @Mock WorkflowEvidencePort shipmentEvidencePort;
    @Mock ObjectStoragePort objectStoragePort;
    @Mock WorkflowAuthorizationSupport authorizationSupport;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);
    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

    private PurchaseClaimAdminService service;

    private static final AuthPrincipal ADMIN = new AuthPrincipal(
        "token1", "admin1", "t1",
        VerificationLevel.PHONE_VERIFIED,
        Set.of("SCOPE_PURCHASE_CLAIM_APPROVE"),
        Instant.parse("2026-03-02T00:00:00Z")
    );

    private PurchaseClaim submittedClaim() {
        return new PurchaseClaim(
            "claim-1",
            "consumer1",
            "SN-001",
            "Model X",
            "eg-1",
            "note",
            PurchaseClaimStatus.SUBMITTED,
            Instant.parse("2026-03-01T09:00:00Z"),
            null,
            null,
            null,
            null,
            null
        );
    }

    @BeforeEach
    void setUp() {
        service = new PurchaseClaimAdminService(
            purchaseClaimRepository, productMintUseCase, ownershipUpdatePort,
            ledgerOutboxPort, shipmentEvidencePort, objectStoragePort, authorizationSupport, clock
        );
    }

    @Test
    void approve_success() {
        PurchaseClaim claim = submittedClaim();
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(purchaseClaimRepository.findById("claim-1")).thenReturn(Optional.of(claim));
        when(productMintUseCase.mint(any(ActorContext.class), any(MintProductCommand.class)))
            .thenReturn(new MintedProductResult("asset-1", "passport-1", "QR-ABC", "outbox-1", "GENESIS", "MINTED"));
        doNothing().when(ownershipUpdatePort).upsertOwner(anyString(), anyString(), any(Instant.class));
        when(purchaseClaimRepository.save(any(PurchaseClaim.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shipmentEvidencePort.findReadyEvidenceHashes("eg-1")).thenReturn(List.of("hash1"));
        when(ledgerOutboxPort.enqueue(any(WorkflowLedgerEventEnvelope.class))).thenReturn("outbox-2");

        ApprovePurchaseClaimResult result = service.approve(
            ADMIN, "claim-1",
            new ApprovePurchaseClaimCommand(Instant.parse("2025-01-01T00:00:00Z"), "B1", "F1")
        );

        assertEquals("APPROVED", result.status());
        assertEquals("passport-1", result.passportId());
        assertEquals("asset-1", result.assetId());
        assertEquals("QR-ABC", result.qrPublicCode());
        verify(ownershipUpdatePort).upsertOwner("passport-1", "consumer1", NOW);
    }

    @Test
    void reject_success() {
        PurchaseClaim claim = submittedClaim();
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(purchaseClaimRepository.findById("claim-1")).thenReturn(Optional.of(claim));
        when(purchaseClaimRepository.save(any(PurchaseClaim.class))).thenAnswer(inv -> inv.getArgument(0));

        RejectPurchaseClaimResult result = service.reject(ADMIN, "claim-1", "시리얼 번호 불일치");

        assertEquals("REJECTED", result.status());
        assertEquals("시리얼 번호 불일치", result.rejectionReason());
        verify(productMintUseCase, never()).mint(any(), any());
        verify(ownershipUpdatePort, never()).upsertOwner(anyString(), anyString(), any());
    }

    @Test
    void approve_alreadyApproved_throws() {
        PurchaseClaim claim = submittedClaim().approve("admin1", "p1", "a1", NOW);
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(purchaseClaimRepository.findById("claim-1")).thenReturn(Optional.of(claim));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.approve(ADMIN, "claim-1",
                new ApprovePurchaseClaimCommand(Instant.now(), "B1", "F1"))
        );
        assertEquals(WorkflowErrorCode.CLAIM_INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void approve_notFound_throws() {
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(purchaseClaimRepository.findById("missing")).thenReturn(Optional.empty());

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.approve(ADMIN, "missing",
                new ApprovePurchaseClaimCommand(Instant.now(), "B1", "F1"))
        );
        assertEquals(WorkflowErrorCode.CLAIM_NOT_FOUND, ex.getErrorCode());
    }

}
