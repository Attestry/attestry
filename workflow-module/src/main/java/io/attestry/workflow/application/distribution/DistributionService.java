package io.attestry.workflow.application.distribution;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.distribution.assembler.DistributionViewAssembler;
import io.attestry.workflow.application.delegation.command.GrantDelegationCommand;
import io.attestry.workflow.application.delegation.result.DelegationResult;
import io.attestry.workflow.application.port.common.TenantReadPort;
import io.attestry.workflow.application.port.distribution.DistributionCandidateQueryPort;
import io.attestry.workflow.application.port.distribution.DistributionCandidateQueryPort.PagedDistributionCandidateResult;
import io.attestry.workflow.application.port.common.WorkflowProjectionOutboxPort;
import io.attestry.workflow.application.port.distribution.DistributionQueryPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.event.WorkflowLedgerEvents;
import io.attestry.workflow.application.usecase.DelegationUseCase;
import io.attestry.workflow.application.usecase.DistributionUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.distribution.model.Distribution;
import io.attestry.workflow.domain.distribution.repository.DistributionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class DistributionService implements DistributionUseCase {

    private final DelegationUseCase delegationUseCase;
    private final DistributionRepository distributionRepository;
    private final DistributionCandidateQueryPort distributionCandidateQueryPort;
    private final DistributionQueryPort distributionQueryPort;
    private final TenantReadPort tenantReadPort;
    private final DistributionViewAssembler viewAssembler;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final WorkflowProjectionOutboxPort projectionOutboxPort;
    private final Clock clock;

    @Override
    @Transactional
    public BatchDistributeResult distribute(
        AuthPrincipal principal,
        String sourceTenantId,
        String partnerLinkId,
        DistributeCommand command
    ) {
        assertDistributionGrantAccess(principal, sourceTenantId, "distribution:distribute:" + partnerLinkId);
        String passportId = requireSinglePassportId(command);
        Instant now = Instant.now(clock);
        BatchDistributeResult.Entry result = distributeOne(
            principal,
            sourceTenantId,
            partnerLinkId,
            passportId,
            command.expiresAt(),
            command.note(),
            now
        );
        return new BatchDistributeResult(List.of(result), 1, 1);
    }

    @Override
    @Transactional
    public DistributionView recall(
        AuthPrincipal principal, String distributionId, RecallCommand command
    ) {
        Instant now = Instant.now(clock);

        Distribution distribution = distributionRepository.findById(distributionId)
            .orElseThrow(() -> new WorkflowDomainException(
                WorkflowErrorCode.DISTRIBUTION_NOT_FOUND, "Distribution not found: " + distributionId
            ));

        assertDistributionGrantAccess(principal, distribution.sourceTenantId(), "distribution:recall:" + distributionId);

        Distribution recalled = distribution.recall(principal.userId(), command.reason(), now);
        distributionRepository.save(recalled);

        projectionOutboxPort.enqueue(WorkflowLedgerEvents.distributionRecalled(recalled));

        delegationUseCase.revoke(principal, distribution.delegationId(), command.reason());

        return distributionQueryPort.findById(distributionId)
            .map(this::toView)
            .orElseThrow(() -> new WorkflowDomainException(
                WorkflowErrorCode.DISTRIBUTION_NOT_FOUND, "Distribution not found: " + distributionId
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedDistributionResponse listByTenant(
        AuthPrincipal principal,
        String sourceTenantId,
        int page,
        int size,
        String keyword
    ) {
        assertDistributionReadAccess(principal, sourceTenantId, "distribution:list:" + sourceTenantId);
        var result = distributionQueryPort.findBySourceTenantId(sourceTenantId, page, size, keyword);
        return viewAssembler.toPagedDistributionResponse(result, loadTargetTenants(result.content()));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedDistributionCandidateResponse listDistributionCandidates(
        AuthPrincipal principal, int page, int size, String keyword
    ) {
        String tenantId = requireTenantId(principal);
        assertDistributionReadAccess(principal, tenantId, "distribution:candidates:" + tenantId);
        PagedDistributionCandidateResult result =
            distributionCandidateQueryPort.findDistributionCandidatesByTenantId(
                tenantId, page, size, keyword
            );
        return viewAssembler.toPagedDistributionCandidateResponse(result);
    }

    private BatchDistributeResult.Entry distributeOne(
        AuthPrincipal principal,
        String sourceTenantId,
        String partnerLinkId,
        String passportId,
        Instant expiresAt,
        String note,
        Instant now
    ) {
        DelegationResult delegationResult = delegationUseCase.grant(
            principal,
            sourceTenantId,
            new GrantDelegationCommand(
                partnerLinkId,
                "PASSPORT",
                passportId,
                "RETAIL_TRANSFER_CREATE",
                expiresAt,
                note
            )
        );

        Distribution distribution = distributionRepository.save(Distribution.create(
            passportId,
            sourceTenantId,
            delegationResult.targetTenantId(),
            partnerLinkId,
            delegationResult.delegationId(),
            principal.userId(),
            now
        ));

        projectionOutboxPort.enqueue(WorkflowLedgerEvents.distributionCreated(distribution));

        return BatchDistributeResult.Entry.success(
            passportId, distribution.distributionId(), delegationResult.delegationId()
        );
    }

    private DistributionView toView(DistributionQueryPort.DistributionRow row) {
        return viewAssembler.toView(
            row,
            tenantReadPort.findTenantSummariesByIds(List.of(row.targetTenantId())).get(row.targetTenantId())
        );
    }

    private java.util.Map<String, TenantReadPort.TenantSummary> loadTargetTenants(
        List<DistributionQueryPort.DistributionRow> rows
    ) {
        return tenantReadPort.findTenantSummariesByIds(
            rows.stream()
                .map(DistributionQueryPort.DistributionRow::targetTenantId)
                .distinct()
                .toList()
        );
    }

    private String requireTenantId(AuthPrincipal principal) {
        if (principal.tenantId() == null || principal.tenantId().isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Tenant-scoped token is required");
        }
        return principal.tenantId();
    }

    private String requireSinglePassportId(DistributeCommand command) {
        if (command.passportIds() == null || command.passportIds().size() != 1) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_REQUEST,
                "Exactly one passportId must be provided"
            );
        }
        String passportId = command.passportIds().get(0);
        if (passportId == null || passportId.isBlank()) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_REQUEST,
                "passportId is required"
            );
        }
        return passportId;
    }

    private void assertDistributionGrantAccess(AuthPrincipal principal, String tenantId, String resourceRef) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.DELEGATION_GRANT, resourceRef);
    }

    private void assertDistributionReadAccess(AuthPrincipal principal, String tenantId, String resourceRef) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.TENANT_READ_ONLY, resourceRef);
    }
}
