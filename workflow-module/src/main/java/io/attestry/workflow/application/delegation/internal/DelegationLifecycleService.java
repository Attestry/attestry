package io.attestry.workflow.application.delegation.internal;

import io.attestry.workflow.application.delegation.command.DelegationEvaluateResult;
import io.attestry.workflow.application.delegation.command.DelegationLifecycleUseCase;
import io.attestry.workflow.application.port.delegation.DelegationPermissionProjectionPort;
import io.attestry.workflow.domain.delegation.model.Delegation;
import io.attestry.workflow.domain.delegation.repository.DelegationRepository;
import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.repository.PartnerLinkRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DelegationLifecycleService implements DelegationLifecycleUseCase {

    private final DelegationRepository delegationRepository;
    private final PartnerLinkRepository partnerLinkRepository;
    private final DelegationPermissionProjectionPort permissionProjectionPort;
    private final Clock clock;

    @Override
    @Transactional
    public DelegationEvaluateResult evaluate(
        String sourceTenantId,
        String targetTenantId,
        String resourceType,
        String resourceId,
        String permissionCode
    ) {
        Delegation delegation = delegationRepository.findActive(
                sourceTenantId,
                targetTenantId,
                resourceType,
                resourceId,
                permissionCode
            )
            .orElse(null);

        if (delegation == null) {
            return new DelegationEvaluateResult(false, "DELEGATION_NOT_FOUND");
        }
        Instant now = Instant.now(clock);
        if (delegation.isExpired(now)) {
            Delegation expired = delegationRepository.save(delegation.expire(now));
            if (expired.isPassportPermissionGrant()) {
                permissionProjectionPort.onDelegationExpired(expired);
            }
            return new DelegationEvaluateResult(false, "DELEGATION_EXPIRED");
        }
        PartnerLink link = partnerLinkRepository.findById(delegation.partnerLinkId()).orElse(null);
        if (link == null || link.status() != PartnerLinkStatus.ACTIVE) {
            return new DelegationEvaluateResult(false, "PARTNER_LINK_NOT_ACTIVE");
        }
        return new DelegationEvaluateResult(true, null);
    }

    @Override
    @Transactional
    public void consumeByPassportId(String passportId) {
        Instant now = Instant.now(clock);
        List<Delegation> activeDelegations = delegationRepository.findActiveByResourceId("PASSPORT", passportId);
        for (Delegation delegation : activeDelegations) {
            Delegation consumed = delegationRepository.save(delegation.consume(now));
            if (consumed.isPassportPermissionGrant()) {
                permissionProjectionPort.onDelegationConsumed(consumed);
            }
        }
    }
}
