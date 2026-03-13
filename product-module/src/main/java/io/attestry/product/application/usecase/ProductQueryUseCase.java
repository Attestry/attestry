package io.attestry.product.application.usecase;

import io.attestry.product.application.dto.view.AssetStateView;
import io.attestry.product.application.dto.view.DistributedPassportDetailView;
import io.attestry.product.application.dto.view.MyPassportView;
import io.attestry.product.application.dto.view.OwnerView;
import io.attestry.product.application.dto.view.PagedDistributedPassportView;
import io.attestry.product.application.dto.view.PagedTenantPassportView;
import io.attestry.product.application.dto.view.PassportDetailView;
import java.time.Instant;
import java.util.List;

public interface ProductQueryUseCase {

    AssetStateView getAssetState(String passportId);

    OwnerView getCurrentOwner(String passportId);

    boolean hasActivePermission(String passportId, String sellerTenantId);

    List<MyPassportView> listMyPassports(String ownerId);

    PassportDetailView getTenantPassportDetail(String tenantId, String passportId);

    DistributedPassportDetailView getDistributedPassportDetail(String tenantId, String passportId);

    DistributedPassportDetailView getCompletedTransferDetail(String tenantId, String passportId);

    PagedTenantPassportView listTenantPassports(
        String tenantId,
        int page,
        int size,
        String assetState,
        Instant createdFrom,
        Instant createdTo,
        String keyword
    );

    PagedDistributedPassportView listDistributedPassports(
        String tenantId,
        int page,
        int size,
        String keyword,
        String sourceTenantId
    );
}
