package io.attestry.product.interfaces.http.dto.response;

import io.attestry.product.application.dto.result.PagedTenantPassportResult;
import java.util.List;

public record PagedTenantPassportResponse(
    List<TenantPassportResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static PagedTenantPassportResponse from(PagedTenantPassportResult result) {
        List<TenantPassportResponse> content = result.content().stream()
            .map(TenantPassportResponse::from)
            .toList();
        return new PagedTenantPassportResponse(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
