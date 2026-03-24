package io.attestry.product.interfaces.http.query.dto.response;

import io.attestry.product.application.query.view.PagedTenantPassportView;
import java.util.List;

public record PagedTenantPassportResponse(
    List<TenantPassportResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static PagedTenantPassportResponse from(PagedTenantPassportView result) {
        List<TenantPassportResponse> content = result.content().stream()
            .map(TenantPassportResponse::from)
            .toList();
        return new PagedTenantPassportResponse(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
