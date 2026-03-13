package io.attestry.workflow.interfaces.transfer.dto.response;

import java.util.List;

public record PagedCompletedTransferResponse(
        List<CompletedTransferResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
