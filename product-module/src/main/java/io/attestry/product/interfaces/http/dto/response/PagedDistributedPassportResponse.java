package io.attestry.product.interfaces.http.dto.response;

import io.attestry.product.application.dto.result.PagedDistributedPassportResult;
import java.util.List;

public record PagedDistributedPassportResponse(
    List<DistributedPassportResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static PagedDistributedPassportResponse from(PagedDistributedPassportResult result) {
        List<DistributedPassportResponse> content = result.content().stream()
            .map(DistributedPassportResponse::from)
            .toList();
        return new PagedDistributedPassportResponse(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
