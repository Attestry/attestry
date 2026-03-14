package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import java.time.Instant;
import java.util.List;

public interface TransferQueryUseCase {

    PagedCompletedTransferResponse listCompletedB2CTransfers(
        AuthPrincipal principal,
        String tenantId,
        String sourceTenantId,
        int page,
        int size
    );

    record CompletedTransferView(
        String transferId,
        String passportId,
        String sourceTenantId,
        String serialNumber,
        String modelName,
        String assetState,
        String toOwnerId,
        String acceptMethod,
        Instant completedAt
    ) {
    }

    record PagedCompletedTransferResponse(
        List<CompletedTransferView> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }
}
