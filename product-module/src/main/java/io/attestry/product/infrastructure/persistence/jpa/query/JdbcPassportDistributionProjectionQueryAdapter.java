package io.attestry.product.infrastructure.persistence.jpa.query;

import io.attestry.product.application.port.query.PassportDistributionQueryPort;
import io.attestry.product.infrastructure.persistence.jpa.entity.PassportDistributionProjectionJpaEntity;
import io.attestry.product.infrastructure.persistence.jpa.repository.PassportDistributionProjectionJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcPassportDistributionProjectionQueryAdapter implements PassportDistributionQueryPort {

    private final PassportDistributionProjectionJpaRepository repository;

    @Override
    public Optional<DistributionRecord> findLatestDistribution(String passportId) {
        return repository.findById(passportId)
            .map(this::toView);
    }

    private DistributionRecord toView(PassportDistributionProjectionJpaEntity entity) {
        return new DistributionRecord(
            entity.getDistributionId(),
            entity.getTargetTenantId(),
            entity.getTargetTenantName(),
            entity.getTargetTenantType(),
            entity.getPartnerLinkId(),
            entity.getStatus(),
            entity.getDistributedAt()
        );
    }
}
