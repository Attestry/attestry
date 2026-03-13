package io.attestry.product.infrastructure.persistence.jpa.query;

import io.attestry.product.application.port.query.GroupPassportQueryPort;
import io.attestry.product.infrastructure.persistence.jpa.repository.ProductPassportJpaRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcGroupPassportQueryAdapter implements GroupPassportQueryPort {

    private final ProductPassportJpaRepository productPassportRepository;

    @Override
    public PagedResult findByTenant(
        String tenantId,
        int page,
        int size,
        String assetState,
        Instant createdFrom,
        Instant createdTo,
        String keyword
    ) {
        return productPassportRepository.findByTenantWithFilters(
            tenantId, page, size, assetState, createdFrom, createdTo, keyword
        );
    }
}
