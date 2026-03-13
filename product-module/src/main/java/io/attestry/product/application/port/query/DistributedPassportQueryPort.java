package io.attestry.product.application.port.query;

import io.attestry.product.application.dto.view.DistributedPassportDetailView;
import io.attestry.product.application.dto.view.DistributedPassportView;
import java.util.List;

public interface DistributedPassportQueryPort {

    PagedResult findByTargetTenant(
        String tenantId,
        int page,
        int size,
        String keyword,
        String sourceTenantId
    );

    DistributedPassportDetailView findDetailByRetailAccess(String tenantId, String passportId);

    DistributedPassportDetailView findDetailByCompletedTransfer(String tenantId, String passportId);

    record PagedResult(List<DistributedPassportView> content, int page, int size, long totalElements, int totalPages) {
    }
}
