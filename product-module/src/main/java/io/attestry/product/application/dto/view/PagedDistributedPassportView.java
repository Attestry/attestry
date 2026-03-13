package io.attestry.product.application.dto.view;

import java.util.List;

public record PagedDistributedPassportView(
    List<DistributedPassportView> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
