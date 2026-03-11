package io.attestry.product.infrastructure.persistence.jpa.repository;

import io.attestry.product.application.port.projection.ProductRetailAccessProjectionPort;

public interface RetailAccessCustomRepository {

    ProductRetailAccessProjectionPort.PagedRetailAccessResult findAccessiblePassportsWithFilters(
        String tenantId,
        int page,
        int size,
        String keyword,
        String sourceTenantId
    );
}
