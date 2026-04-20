package io.attestry.workflow.application.distribution.command;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.delegation.command.GrantDelegationCommand;
import io.attestry.workflow.application.delegation.command.DelegationResult;
import io.attestry.workflow.application.distribution.result.BatchDistributeResult;
import io.attestry.workflow.application.distribution.internal.DistributionLookupService;
import io.attestry.workflow.application.distribution.internal.DistributionViewReader;
import io.attestry.workflow.application.delegation.command.DelegationUseCase;
import io.attestry.workflow.application.distribution.view.DistributionView;
import io.attestry.workflow.application.port.common.TenantReadPort;
import io.attestry.workflow.application.port.common.WorkflowProjectionOutboxPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.event.WorkflowLedgerEvents;
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
public class DistributionCommandService implements DistributionCommandUseCase {

    private final DelegationUseCase delegationUseCase;
    private final DistributionRepository distributionRepository;
    private final DistributionLookupService distributionLookupService;
    private final DistributionViewReader distributionViewReader;
    private final TenantReadPort tenantReadPort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final WorkflowProjectionOutboxPort projectionOutboxPort;
    private final Clock clock;

    @Override
    @Transactional
    public BatchDistributeResult distribute(
        WorkflowActorContext principal,
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
        WorkflowActorContext principal,
        String distributionId,
        RecallDistributionCommand command
    ) {
        Instant now = Instant.now(clock);

        Distribution distribution = distributionLookupService.getById(distributionId);

        assertDistributionGrantAccess(principal, distribution.sourceTenantId(), "distribution:recall:" + distributionId);

        Distribution recalled = distribution.recall(principal.userId(), command.reason(), now);
        distributionRepository.save(recalled);

        TenantReadPort.TenantSummary targetTenant = tenantReadPort.findTenantSummary(distribution.targetTenantId());
        projectionOutboxPort.enqueue(WorkflowLedgerEvents.distributionRecalled(
            recalled, targetTenant.name(), targetTenant.type()
        ));

        delegationUseCase.revoke(principal, distribution.delegationId(), command.reason());
        return distributionViewReader.resolveById(distributionId);
    }

    private BatchDistributeResult.Entry distributeOne(
        WorkflowActorContext principal,
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

        TenantReadPort.TenantSummary targetTenant = tenantReadPort.findTenantSummary(distribution.targetTenantId());
        projectionOutboxPort.enqueue(WorkflowLedgerEvents.distributionCreated(
            distribution, targetTenant.name(), targetTenant.type()
        ));

        return BatchDistributeResult.Entry.success(
            passportId, distribution.distributionId(), delegationResult.delegationId()
        );
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

    private void assertDistributionGrantAccess(WorkflowActorContext principal, String tenantId, String resourceRef) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.DELEGATION_GRANT, resourceRef);
    }
}
