package io.attestry.product.infrastructure.persistence.jpa.repository;

import io.attestry.product.application.port.query.GroupPassportQueryPort;
import java.time.Instant;

public interface GroupPassportCustomRepository {

    GroupPassportQueryPort.PagedResult findByTenantWithFilters(
        String tenantId,
        int page,
        int size,
        String assetState,
        Instant createdFrom,
        Instant createdTo,
        String keyword
    );
}
