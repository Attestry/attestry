package io.attestry.product.interfaces.http.query.dto.response;

import io.attestry.product.application.dto.view.PagedDistributedPassportView;
import java.util.List;

public record PagedDistributedPassportResponse(
    List<DistributedPassportResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static PagedDistributedPassportResponse from(PagedDistributedPassportView result) {
        List<DistributedPassportResponse> content = result.content().stream()
            .map(DistributedPassportResponse::from)
            .toList();
        return new PagedDistributedPassportResponse(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
