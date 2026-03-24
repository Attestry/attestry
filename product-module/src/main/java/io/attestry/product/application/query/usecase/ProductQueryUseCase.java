package io.attestry.product.application.query.usecase;

import io.attestry.product.application.query.view.AssetStateView;
import io.attestry.product.application.query.view.DistributedPassportDetailView;
import io.attestry.product.application.query.view.MyPassportView;
import io.attestry.product.application.query.view.OwnerView;
import io.attestry.product.application.query.view.PagedDistributedPassportView;
import io.attestry.product.application.query.view.PagedTenantPassportView;
import io.attestry.product.application.query.view.PassportDetailView;
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
