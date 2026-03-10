package io.attestry.product.application.dto.result;

import java.util.List;

public record PagedDistributedPassportResult(
    List<DistributedPassportResult> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
