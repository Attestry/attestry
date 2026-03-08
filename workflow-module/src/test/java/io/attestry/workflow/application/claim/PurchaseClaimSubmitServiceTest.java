package io.attestry.workflow.application.claim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.attestry.userauth.application.port.ObjectStoragePort;
import io.attestry.userauth.domain.identity.model.VerificationLevel;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.claim.command.SubmitPurchaseClaimCommand;
import io.attestry.workflow.application.claim.result.SubmitPurchaseClaimResult;
import io.attestry.workflow.application.port.WorkflowEvidencePort;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import io.attestry.workflow.domain.claim.repository.PurchaseClaimRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseClaimSubmitServiceTest {

    @Mock PurchaseClaimRepository purchaseClaimRepository;
    @Mock WorkflowEvidencePort shipmentEvidencePort;
    @Mock PurchaseClaimEvidenceService evidenceService;
    @Mock ObjectStoragePort objectStoragePort;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);

    private PurchaseClaimSubmitService service;

    private static final AuthPrincipal CONSUMER = new AuthPrincipal(
        "token1", "consumer1", null,
        VerificationLevel.PHONE_VERIFIED, Set.of(), Instant.parse("2026-03-02T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new PurchaseClaimSubmitService(
            purchaseClaimRepository, shipmentEvidencePort, evidenceService, objectStoragePort, clock
        );
    }

    @Test
    void submit_success() {
        when(shipmentEvidencePort.findEvidenceGroupScope("eg-1"))
            .thenReturn(java.util.Optional.of(new WorkflowEvidencePort.EvidenceGroupScopeView("eg-1", "t1", "consumer1")));
        when(shipmentEvidencePort.findReadyEvidenceHashes("eg-1"))
            .thenReturn(List.of("abc123"));
        when(purchaseClaimRepository.save(any(PurchaseClaim.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SubmitPurchaseClaimResult result = service.submit(
            CONSUMER,
            new SubmitPurchaseClaimCommand("SN-001", "Model X", "eg-1", "note")
        );

        assertEquals("SUBMITTED", result.status());
        assertNotNull(result.claimId());
        assertNotNull(result.submittedAt());
    }

    @Test
    void submit_noEvidence_throws() {
        when(shipmentEvidencePort.findEvidenceGroupScope("eg-1"))
            .thenReturn(java.util.Optional.of(new WorkflowEvidencePort.EvidenceGroupScopeView("eg-1", "t1", "consumer1")));
        when(shipmentEvidencePort.findReadyEvidenceHashes("eg-1"))
            .thenReturn(List.of());

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.submit(
                CONSUMER,
                new SubmitPurchaseClaimCommand("SN-001", "Model X", "eg-1", null)
            )
        );
        assertEquals(WorkflowErrorCode.CLAIM_EVIDENCE_INSUFFICIENT, ex.getErrorCode());
    }

    @Test
    void submit_noUser_throws() {
        AuthPrincipal noUser = new AuthPrincipal(
            "token1", null, null,
            VerificationLevel.PHONE_VERIFIED, Set.of(), Instant.parse("2026-03-02T00:00:00Z")
        );

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.submit(
                noUser,
                new SubmitPurchaseClaimCommand("SN-001", "Model X", "eg-1", null)
            )
        );
        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }
}
