package io.attestry.workflow.infrastructure.persistence.jpa.distribution;

import io.attestry.workflow.application.port.distribution.DistributionCandidateQueryPort;
import io.attestry.workflow.infrastructure.persistence.jpa.repository.DistributionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaDistributionCandidateQueryAdapter implements DistributionCandidateQueryPort {

    private final DistributionJpaRepository distributionJpaRepository;

    @Override
    public PagedDistributionCandidateResult findDistributionCandidatesByTenantId(
        String tenantId, int page, int size, String keyword
    ) {
        Page<DistributionJpaRepository.DistributionCandidateProjection> result =
            distributionJpaRepository.findDistributionCandidatesByTenantId(
                tenantId,
                normalizeKeyword(keyword),
                PageRequest.of(page, size)
            );

        return new PagedDistributionCandidateResult(
            result.getContent().stream()
                .map(candidate -> new DistributionCandidate(
                    candidate.getPassportId(),
                    candidate.getAssetId(),
                    candidate.getSerialNumber(),
                    candidate.getModelId(),
                    candidate.getModelName(),
                    candidate.getProductionBatch(),
                    candidate.getFactoryCode()
                ))
                .toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase();
    }
}
