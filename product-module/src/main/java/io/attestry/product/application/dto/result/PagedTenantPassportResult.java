package io.attestry.product.application.dto.result;

import java.util.List;

public record PagedTenantPassportResult(
    List<TenantPassportResult> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
