package io.attestry.workflow.application.claim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.attestry.userauth.domain.auth.model.VerificationLevel;
import io.attestry.workflow.application.claim.command.PurchaseClaimEvidenceService;
import io.attestry.workflow.application.claim.command.PurchaseClaimSubmitService;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.claim.command.SubmitPurchaseClaimCommand;
import io.attestry.workflow.application.claim.result.SubmitPurchaseClaimResult;
import io.attestry.workflow.application.claim.internal.PurchaseClaimEvidenceViewResolver;
import io.attestry.workflow.application.claim.internal.PurchaseClaimLookupService;
import io.attestry.workflow.application.claim.internal.PurchaseClaimViewFactory;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
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
    @Mock PurchaseClaimLookupService claimLookupService;
    @Mock PurchaseClaimEvidenceViewResolver evidenceViewResolver;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);

    private PurchaseClaimSubmitService service;

    private static final WorkflowActorContext CONSUMER = new WorkflowActorContext(
        "token1", "consumer1", null,
        VerificationLevel.PHONE_VERIFIED, Set.of(), Instant.parse("2026-03-02T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new PurchaseClaimSubmitService(
            purchaseClaimRepository,
            shipmentEvidencePort,
            evidenceService,
            claimLookupService,
            evidenceViewResolver,
            new PurchaseClaimViewFactory(),
            clock
        );
    }

    @Test
    void submit_success() {
        when(shipmentEvidencePort.findEvidenceGroupScope("eg-1"))
            .thenReturn(java.util.Optional.of(new WorkflowEvidencePort.EvidenceGroupScopeRecord("eg-1", "t1", "consumer1")));
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
            .thenReturn(java.util.Optional.of(new WorkflowEvidencePort.EvidenceGroupScopeRecord("eg-1", "t1", "consumer1")));
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
        WorkflowActorContext noUser = new WorkflowActorContext(
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
