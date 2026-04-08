package io.attestry.workflow.application.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.auth.model.VerificationLevel;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.delegation.command.GrantDelegationCommand;
import io.attestry.workflow.application.delegation.result.DelegationResult;
import io.attestry.workflow.application.delegation.command.DelegationUseCase;
import io.attestry.workflow.application.distribution.command.DistributeCommand;
import io.attestry.workflow.application.distribution.command.DistributionCommandService;
import io.attestry.workflow.application.distribution.command.RecallDistributionCommand;
import io.attestry.workflow.application.distribution.internal.DistributionLookupService;
import io.attestry.workflow.application.distribution.internal.DistributionViewAssembler;
import io.attestry.workflow.application.distribution.internal.DistributionViewReader;
import io.attestry.workflow.application.distribution.query.DistributionQueryService;
import io.attestry.workflow.application.distribution.result.BatchDistributeResult;
import io.attestry.workflow.application.distribution.view.DistributionView;
import io.attestry.workflow.application.distribution.view.PagedDistributionView;
import io.attestry.workflow.application.port.common.TenantReadPort;
import io.attestry.workflow.application.port.distribution.DistributionCandidateQueryPort;
import io.attestry.workflow.application.port.common.WorkflowProjectionOutboxPort;
import io.attestry.workflow.application.port.distribution.DistributionQueryPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.distribution.model.Distribution;
import io.attestry.workflow.domain.distribution.repository.DistributionRepository;
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
class DistributionServiceTest {

    @Mock DelegationUseCase delegationUseCase;
    @Mock DistributionRepository distributionRepository;
    @Mock DistributionCandidateQueryPort distributionCandidateQueryPort;
    @Mock DistributionQueryPort distributionQueryPort;
    @Mock TenantReadPort tenantReadPort;
    @Mock DistributionViewAssembler viewAssembler;
    @Mock WorkflowAuthorizationSupport authorizationSupport;
    @Mock WorkflowProjectionOutboxPort projectionOutboxPort;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);

    private DistributionCommandService commandService;
    private DistributionQueryService queryService;

    private static final String SOURCE_TENANT = "source-tenant";
    private static final String TARGET_TENANT = "target-tenant";
    private static final String PARTNER_LINK_ID = "pl-1";
    private static final Instant EXPIRES = Instant.parse("2026-04-01T00:00:00Z");
    private static final WorkflowActorContext PRINCIPAL = new WorkflowActorContext(
        "token1", "admin1", SOURCE_TENANT,
        VerificationLevel.PHONE_VERIFIED, Set.of("SCOPE_DELEGATION_GRANT"),
        Instant.parse("2026-03-02T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        commandService = new DistributionCommandService(
            delegationUseCase,
            distributionRepository,
            new DistributionLookupService(distributionRepository),
            new DistributionViewReader(distributionQueryPort, tenantReadPort, viewAssembler),
            tenantReadPort,
            authorizationSupport,
            projectionOutboxPort,
            clock
        );
        queryService = new DistributionQueryService(
            distributionCandidateQueryPort,
            distributionQueryPort,
            tenantReadPort,
            viewAssembler,
            authorizationSupport
        );
    }

    @Test
    void distribute_allSuccess() {
        stubDistributionGrantAuthorization();
        when(delegationUseCase.grant(any(), any(), any())).thenAnswer(inv -> {
            GrantDelegationCommand cmd = inv.getArgument(2);
            return delegationResultFor(cmd.resourceId());
        });
        when(distributionRepository.save(any(Distribution.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tenantReadPort.findTenantSummary(TARGET_TENANT))
            .thenReturn(new TenantReadPort.TenantSummary(TARGET_TENANT, "Target", "", "", "RETAIL"));

        BatchDistributeResult result = commandService.distribute(
            PRINCIPAL, SOURCE_TENANT, PARTNER_LINK_ID,
            new DistributeCommand(List.of("p1"), EXPIRES, "batch note")
        );

        assertEquals(1, result.totalRequested());
        assertEquals(1, result.totalDistributed());
        assertEquals("DISTRIBUTED", result.results().get(0).status());
        verify(delegationUseCase, times(1)).grant(any(), any(), any());
        verify(distributionRepository, times(1)).save(any(Distribution.class));
    }

    @Test
    void distribute_failure_propagatesDelegationException() {
        stubDistributionGrantAuthorization();
        when(delegationUseCase.grant(any(), any(), any()))
            .thenThrow(new WorkflowDomainException(WorkflowErrorCode.DELEGATION_ALREADY_ACTIVE, "Already active"));

        WorkflowDomainException ex = org.junit.jupiter.api.Assertions.assertThrows(
            WorkflowDomainException.class,
            () -> commandService.distribute(
                PRINCIPAL,
                SOURCE_TENANT,
                PARTNER_LINK_ID,
                new DistributeCommand(List.of("p1"), EXPIRES, "note")
            )
        );

        assertEquals(WorkflowErrorCode.DELEGATION_ALREADY_ACTIVE, ex.getErrorCode());
        verify(distributionRepository, never()).save(any(Distribution.class));
    }

    @Test
    void distribute_rejectsNonSingleRequest() {
        stubDistributionGrantAuthorization();
        WorkflowDomainException ex = org.junit.jupiter.api.Assertions.assertThrows(
            WorkflowDomainException.class,
            () -> commandService.distribute(
                PRINCIPAL,
                SOURCE_TENANT,
                PARTNER_LINK_ID,
                new DistributeCommand(List.of(), EXPIRES, "note")
            )
        );

        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
        verify(delegationUseCase, never()).grant(any(), any(), any());
        verify(distributionRepository, never()).save(any(Distribution.class));
    }

    @Test
    void listByTenant_enrichesTargetTenantWithBatchLookup() {
        DistributionQueryPort.DistributionRow row = new DistributionQueryPort.DistributionRow(
            "dist-1",
            "passport-1",
            SOURCE_TENANT,
            TARGET_TENANT,
            PARTNER_LINK_ID,
            "delegation-1",
            "DISTRIBUTED",
            "SN-1",
            "Model X",
            "user-1",
            EXPIRES,
            null,
            null,
            null
        );
        DistributionView view = new DistributionView(
            "dist-1",
            "passport-1",
            SOURCE_TENANT,
            TARGET_TENANT,
            "Target Tenant",
            "RETAIL",
            PARTNER_LINK_ID,
            "delegation-1",
            "DISTRIBUTED",
            "SN-1",
            "Model X",
            "user-1",
            EXPIRES,
            null,
            null,
            null
        );

        stubTenantReadAuthorization(SOURCE_TENANT, "distribution:list:" + SOURCE_TENANT);
        when(distributionQueryPort.findBySourceTenantId(SOURCE_TENANT, 0, 20, "abc"))
            .thenReturn(new DistributionQueryPort.PagedDistributionResult(List.of(row), 0, 20, 1, 1));
        when(tenantReadPort.findTenantSummariesByIds(List.of(TARGET_TENANT)))
            .thenReturn(java.util.Map.of(
                TARGET_TENANT,
                new TenantReadPort.TenantSummary(TARGET_TENANT, "Target Tenant", "KR", "addr", "RETAIL")
            ));
        when(viewAssembler.toPagedDistributionResponse(any(), any()))
            .thenReturn(new PagedDistributionView(
                List.of(view), 0, 20, 1, 1
            ));

        var result = queryService.listByTenant(PRINCIPAL, SOURCE_TENANT, 0, 20, "abc");

        assertEquals(1, result.content().size());
        verify(tenantReadPort).findTenantSummariesByIds(List.of(TARGET_TENANT));
        verify(viewAssembler).toPagedDistributionResponse(any(), any());
    }

    @Test
    void recall_checksSourceTenantAuthorizationBeforeRevokingDelegation() {
        Distribution distribution = Distribution.create(
            "passport-1",
            SOURCE_TENANT,
            TARGET_TENANT,
            PARTNER_LINK_ID,
            "delegation-1",
            "admin1",
            Instant.parse("2026-03-01T09:00:00Z")
        );
        Distribution recalled = distribution.recall("admin1", "reason", Instant.parse("2026-03-01T10:00:00Z"));
        DistributionQueryPort.DistributionRow row = new DistributionQueryPort.DistributionRow(
            recalled.distributionId(),
            recalled.passportId(),
            recalled.sourceTenantId(),
            recalled.targetTenantId(),
            recalled.partnerLinkId(),
            recalled.delegationId(),
            recalled.status().name(),
            "SN-1",
            "Model X",
            recalled.distributedByUserId(),
            recalled.distributedAt(),
            recalled.recalledByUserId(),
            recalled.recalledAt(),
            recalled.recallReason()
        );
        when(distributionRepository.findById(recalled.distributionId())).thenReturn(java.util.Optional.of(distribution));
        when(distributionRepository.save(any(Distribution.class))).thenReturn(recalled);
        when(tenantReadPort.findTenantSummary(TARGET_TENANT))
            .thenReturn(new TenantReadPort.TenantSummary(TARGET_TENANT, "Target Tenant", "", "", "RETAIL"));
        stubDistributionGrantAuthorization(SOURCE_TENANT, "distribution:recall:" + recalled.distributionId());
        when(delegationUseCase.revoke(PRINCIPAL, "delegation-1", "reason"))
            .thenReturn(new DelegationResult("delegation-1", PARTNER_LINK_ID, SOURCE_TENANT, TARGET_TENANT, "PASSPORT", "passport-1", "RETAIL_TRANSFER_CREATE", "REVOKED", null, "reason"));
        when(distributionQueryPort.findById(recalled.distributionId())).thenReturn(Optional.of(row));
        when(tenantReadPort.findTenantSummariesByIds(List.of(TARGET_TENANT)))
            .thenReturn(java.util.Map.of(
                TARGET_TENANT,
                new TenantReadPort.TenantSummary(TARGET_TENANT, "Target Tenant", "KR", "addr", "RETAIL")
            ));
        when(viewAssembler.toView(any(), any())).thenReturn(new DistributionView(
            recalled.distributionId(),
            recalled.passportId(),
            SOURCE_TENANT,
            TARGET_TENANT,
            "Target Tenant",
            "RETAIL",
            PARTNER_LINK_ID,
            "delegation-1",
            "RECALLED",
            "SN-1",
            "Model X",
            recalled.distributedByUserId(),
            recalled.distributedAt(),
            recalled.recalledByUserId(),
            recalled.recalledAt(),
            recalled.recallReason()
        ));

        DistributionView result = commandService.recall(PRINCIPAL, recalled.distributionId(), new RecallDistributionCommand("reason"));

        assertEquals("RECALLED", result.status());
        verify(authorizationSupport).assertTenantContext(PRINCIPAL, SOURCE_TENANT);
        verify(authorizationSupport).assertLivePermission(PRINCIPAL, SOURCE_TENANT, PermissionCodes.DELEGATION_GRANT, "distribution:recall:" + recalled.distributionId());
    }

    private DelegationResult delegationResultFor(String passportId) {
        return new DelegationResult(
            "del-" + passportId, PARTNER_LINK_ID,
            SOURCE_TENANT, TARGET_TENANT,
            "PASSPORT", passportId, "RETAIL_TRANSFER_CREATE",
            "ACTIVE", EXPIRES, null
        );
    }

    private void stubDistributionGrantAuthorization() {
        stubDistributionGrantAuthorization(SOURCE_TENANT, "distribution:distribute:" + PARTNER_LINK_ID);
    }

    private void stubDistributionGrantAuthorization(String tenantId, String resourceRef) {
        doNothing().when(authorizationSupport).assertTenantContext(PRINCIPAL, tenantId);
        doNothing().when(authorizationSupport)
            .assertLivePermission(PRINCIPAL, tenantId, PermissionCodes.DELEGATION_GRANT, resourceRef);
    }

    private void stubTenantReadAuthorization(String tenantId, String resourceRef) {
        doNothing().when(authorizationSupport).assertTenantContext(PRINCIPAL, tenantId);
        doNothing().when(authorizationSupport)
            .assertLivePermission(PRINCIPAL, tenantId, PermissionCodes.TENANT_READ_ONLY, resourceRef);
    }
}
