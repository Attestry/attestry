package io.attestry.product.application.usecase;

import io.attestry.product.application.dto.result.AssetStateResult;
import io.attestry.product.application.dto.result.DistributedPassportDetailResult;
import io.attestry.product.application.dto.result.DistributedPassportResult;
import io.attestry.product.application.dto.result.MyPassportResult;
import io.attestry.product.application.dto.result.OwnerResult;
import io.attestry.product.application.dto.result.PagedDistributedPassportResult;
import io.attestry.product.application.dto.result.PagedTenantPassportResult;
import io.attestry.product.application.dto.result.PassportDetailResult;
import io.attestry.product.application.dto.result.TenantPassportResult;
import java.time.Instant;
import java.util.List;

public interface ProductQueryUseCase {

    AssetStateResult getAssetState(String passportId);

    OwnerResult getCurrentOwner(String passportId);

    boolean hasActivePermission(String passportId, String sellerTenantId);

    List<MyPassportResult> listMyPassports(String ownerId);

    PassportDetailResult getTenantPassportDetail(String tenantId, String passportId);

    DistributedPassportDetailResult getDistributedPassportDetail(String tenantId, String passportId);

    PagedTenantPassportResult listTenantPassports(
        String tenantId,
        int page,
        int size,
        String assetState,
        Instant createdFrom,
        Instant createdTo,
        String keyword
    );

    PagedDistributedPassportResult listDistributedPassports(
        String tenantId,
        int page,
        int size,
        String keyword,
        String sourceTenantId
    );
}
