package io.attestry.workflow.application.transfer.view;

import java.util.List;

public record PagedCompletedTransferView(
    List<CompletedTransferView> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
