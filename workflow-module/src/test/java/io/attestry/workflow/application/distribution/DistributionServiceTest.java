package io.attestry.workflow.application.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.userauth.domain.identity.model.VerificationLevel;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.delegation.command.GrantDelegationCommand;
import io.attestry.workflow.application.delegation.result.DelegationResult;
import io.attestry.workflow.application.distribution.assembler.DistributionViewAssembler;
import io.attestry.workflow.application.port.distribution.DistributionCandidateQueryPort;
import io.attestry.workflow.application.port.distribution.DistributionQueryPort;
import io.attestry.workflow.application.usecase.DelegationUseCase;
import io.attestry.workflow.application.usecase.DistributionUseCase.BatchDistributeResult;
import io.attestry.workflow.application.usecase.DistributionUseCase.DistributeCommand;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.distribution.model.Distribution;
import io.attestry.workflow.domain.distribution.repository.DistributionRepository;
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
class DistributionServiceTest {

    @Mock DelegationUseCase delegationUseCase;
    @Mock DistributionRepository distributionRepository;
    @Mock DistributionCandidateQueryPort distributionCandidateQueryPort;
    @Mock DistributionQueryPort distributionQueryPort;
    @Mock DistributionViewAssembler viewAssembler;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);

    private DistributionService service;

    private static final String SOURCE_TENANT = "source-tenant";
    private static final String TARGET_TENANT = "target-tenant";
    private static final String PARTNER_LINK_ID = "pl-1";
    private static final Instant EXPIRES = Instant.parse("2026-04-01T00:00:00Z");
    private static final AuthPrincipal PRINCIPAL = new AuthPrincipal(
        "token1", "admin1", SOURCE_TENANT,
        VerificationLevel.PHONE_VERIFIED, Set.of("SCOPE_DELEGATION_GRANT"),
        Instant.parse("2026-03-02T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new DistributionService(delegationUseCase, distributionRepository, distributionCandidateQueryPort, distributionQueryPort, viewAssembler, clock);
    }

    @Test
    void distribute_allSuccess() {
        when(delegationUseCase.grant(any(), any(), any())).thenAnswer(inv -> {
            GrantDelegationCommand cmd = inv.getArgument(2);
            return delegationResultFor(cmd.resourceId());
        });
        when(distributionRepository.save(any(Distribution.class))).thenAnswer(inv -> inv.getArgument(0));

        BatchDistributeResult result = service.distribute(
            PRINCIPAL, SOURCE_TENANT, PARTNER_LINK_ID,
            new DistributeCommand(List.of("p1", "p2"), EXPIRES, "batch note")
        );

        assertEquals(2, result.totalRequested());
        assertEquals(2, result.totalDistributed());
        assertEquals("DISTRIBUTED", result.results().get(0).status());
        assertEquals("DISTRIBUTED", result.results().get(1).status());
        verify(delegationUseCase, times(2)).grant(any(), any(), any());
        verify(distributionRepository, times(2)).save(any(Distribution.class));
    }

    @Test
    void distribute_partialSuccess_delegationFails() {
        when(delegationUseCase.grant(any(), any(), any())).thenAnswer(inv -> {
            GrantDelegationCommand cmd = inv.getArgument(2);
            if ("p2".equals(cmd.resourceId())) {
                throw new WorkflowDomainException(WorkflowErrorCode.DELEGATION_ALREADY_ACTIVE, "Already active");
            }
            return delegationResultFor(cmd.resourceId());
        });
        when(distributionRepository.save(any(Distribution.class))).thenAnswer(inv -> inv.getArgument(0));

        BatchDistributeResult result = service.distribute(
            PRINCIPAL, SOURCE_TENANT, PARTNER_LINK_ID,
            new DistributeCommand(List.of("p1", "p2"), EXPIRES, "note")
        );

        assertEquals(2, result.totalRequested());
        assertEquals(1, result.totalDistributed());
        assertEquals("DISTRIBUTED", result.results().get(0).status());
        assertEquals("FAILED", result.results().get(1).status());
        assertEquals("DELEGATION_ALREADY_ACTIVE", result.results().get(1).error());
        verify(distributionRepository, times(1)).save(any(Distribution.class));
    }

    @Test
    void distribute_emptyList_returnsEmpty() {
        BatchDistributeResult result = service.distribute(
            PRINCIPAL, SOURCE_TENANT, PARTNER_LINK_ID,
            new DistributeCommand(List.of(), EXPIRES, "note")
        );

        assertEquals(0, result.totalRequested());
        assertEquals(0, result.totalDistributed());
        verify(delegationUseCase, never()).grant(any(), any(), any());
        verify(distributionRepository, never()).save(any(Distribution.class));
    }

    private DelegationResult delegationResultFor(String passportId) {
        return new DelegationResult(
            "del-" + passportId, PARTNER_LINK_ID,
            SOURCE_TENANT, TARGET_TENANT,
            "PASSPORT", passportId, "RETAIL_TRANSFER_CREATE",
            "ACTIVE", EXPIRES, null
        );
    }
}
