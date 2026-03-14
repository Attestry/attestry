package io.attestry.product.application.dto.view;

import java.util.List;

public record PagedTenantPassportView(
    List<TenantPassportView> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
