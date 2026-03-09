package io.attestry.workflow.application.distribution;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.delegation.command.GrantDelegationCommand;
import io.attestry.workflow.application.delegation.result.DelegationResult;
import io.attestry.workflow.application.port.DistributionCandidateQueryPort;
import io.attestry.workflow.application.port.DistributionCandidateQueryPort.PagedDistributionCandidateResult;
import io.attestry.workflow.application.port.DistributionQueryPort;
import io.attestry.workflow.application.port.DistributionQueryPort.DistributionRow;
import io.attestry.workflow.application.usecase.DelegationUseCase;
import io.attestry.workflow.application.usecase.DistributionUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.distribution.model.Distribution;
import io.attestry.workflow.domain.distribution.repository.DistributionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DistributionService implements DistributionUseCase {

    private final DelegationUseCase delegationUseCase;
    private final DistributionRepository distributionRepository;
    private final DistributionCandidateQueryPort distributionCandidateQueryPort;
    private final DistributionQueryPort distributionQueryPort;
    private final Clock clock;

    public DistributionService(
        DelegationUseCase delegationUseCase,
        DistributionRepository distributionRepository,
        DistributionCandidateQueryPort distributionCandidateQueryPort,
        DistributionQueryPort distributionQueryPort,
        Clock clock
    ) {
        this.delegationUseCase = delegationUseCase;
        this.distributionRepository = distributionRepository;
        this.distributionCandidateQueryPort = distributionCandidateQueryPort;
        this.distributionQueryPort = distributionQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BatchDistributeResult distribute(
        AuthPrincipal principal,
        String sourceTenantId,
        String partnerLinkId,
        DistributeCommand command
    ) {
        Instant now = Instant.now(clock);
        List<BatchDistributeResult.Entry> results = new ArrayList<>();

        for (String passportId : command.passportIds()) {
            results.add(distributeSingle(
                principal, sourceTenantId, partnerLinkId, passportId,
                command.expiresAt(), command.note(), now
            ));
        }

        long distributed = results.stream().filter(BatchDistributeResult.Entry::isSuccess).count();
        return new BatchDistributeResult(results, command.passportIds().size(), distributed);
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

        Distribution recalled = distribution.recall(principal.userId(), command.reason(), now);
        distributionRepository.save(recalled);

        delegationUseCase.revoke(principal, distribution.delegationId(), command.reason());

        return distributionQueryPort.findById(distributionId)
            .map(this::toView)
            .orElseThrow(() -> new WorkflowDomainException(
                WorkflowErrorCode.DISTRIBUTION_NOT_FOUND, "Distribution not found: " + distributionId
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedDistributionResponse listByTenant(String sourceTenantId, int page, int size, String keyword) {
        var result = distributionQueryPort.findBySourceTenantId(sourceTenantId, page, size, keyword);
        List<DistributionView> content = result.content().stream()
            .map(this::toView)
            .toList();
        return new PagedDistributionResponse(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedDistributionCandidateResponse listDistributionCandidates(
        AuthPrincipal principal, int page, int size, String keyword
    ) {
        PagedDistributionCandidateResult result =
            distributionCandidateQueryPort.findDistributionCandidatesByTenantId(
                principal.tenantId(), page, size, keyword
            );

        List<DistributionCandidateView> content = result.content().stream()
            .map(c -> new DistributionCandidateView(
                c.passportId(),
                c.assetId(),
                c.serialNumber(),
                c.modelId(),
                c.modelName(),
                c.productionBatch(),
                c.factoryCode()
            ))
            .toList();

        return new PagedDistributionCandidateResponse(
            content, result.page(), result.size(), result.totalElements(), result.totalPages()
        );
    }

    private DistributionView toView(DistributionRow r) {
        return new DistributionView(
            r.distributionId(),
            r.passportId(),
            r.sourceTenantId(),
            r.targetTenantId(),
            r.targetTenantName(),
            r.targetTenantType(),
            r.partnerLinkId(),
            r.delegationId(),
            r.status(),
            r.serialNumber(),
            r.modelName(),
            r.distributedByUserId(),
            r.distributedAt(),
            r.recalledByUserId(),
            r.recalledAt(),
            r.recallReason()
        );
    }

    private BatchDistributeResult.Entry distributeSingle(
        AuthPrincipal principal,
        String sourceTenantId,
        String partnerLinkId,
        String passportId,
        Instant expiresAt,
        String note,
        Instant now
    ) {
        try {
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

            return BatchDistributeResult.Entry.success(
                passportId, distribution.distributionId(), delegationResult.delegationId()
            );
        } catch (WorkflowDomainException ex) {
            return BatchDistributeResult.Entry.failed(passportId, ex.getErrorCode().name());
        }
    }
}
