package io.attestry.workflow.infrastructure.persistence.jpa.distribution;

import io.attestry.workflow.application.port.distribution.DistributionQueryPort;
import io.attestry.workflow.infrastructure.persistence.jpa.repository.DistributionJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaDistributionQueryAdapter implements DistributionQueryPort {

    private final DistributionJpaRepository distributionJpaRepository;

    @Override
    public PagedDistributionResult findBySourceTenantId(String sourceTenantId, int page, int size, String keyword) {
        Page<DistributionJpaRepository.DistributionRowProjection> result =
            distributionJpaRepository.findDistributionRowsBySourceTenantId(
                sourceTenantId,
                normalizeKeyword(keyword),
                PageRequest.of(page, size)
            );

        return new PagedDistributionResult(
            result.getContent().stream().map(this::toRow).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Override
    public Optional<DistributionRow> findById(String distributionId) {
        return distributionJpaRepository.findDistributionRowById(distributionId)
            .map(this::toRow);
    }

    private DistributionRow toRow(DistributionJpaRepository.DistributionRowProjection projection) {
        return new DistributionRow(
            projection.getDistributionId(),
            projection.getPassportId(),
            projection.getSourceTenantId(),
            projection.getTargetTenantId(),
            projection.getPartnerLinkId(),
            projection.getDelegationId(),
            projection.getStatus(),
            projection.getSerialNumber(),
            projection.getModelName(),
            projection.getDistributedByUserId(),
            projection.getDistributedAt(),
            projection.getRecalledByUserId(),
            projection.getRecalledAt(),
            projection.getRecallReason()
        );
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase();
    }
}
