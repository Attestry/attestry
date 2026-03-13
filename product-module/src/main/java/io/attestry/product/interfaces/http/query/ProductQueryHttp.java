package io.attestry.product.interfaces.http.query;

import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.commonlib.web.CurrentActor;
import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.usecase.ProductQueryUseCase;
import io.attestry.product.interfaces.http.query.dto.response.AssetStateResponse;
import io.attestry.product.interfaces.http.query.dto.response.DistributedPassportDetailResponse;
import io.attestry.product.interfaces.http.query.dto.response.MyPassportResponse;
import io.attestry.product.interfaces.http.query.dto.response.OwnerResponse;
import io.attestry.product.interfaces.http.query.dto.response.PagedDistributedPassportResponse;
import io.attestry.product.interfaces.http.query.dto.response.PagedTenantPassportResponse;
import io.attestry.product.interfaces.http.query.dto.response.PassportDetailResponse;
import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/products")
public class ProductQueryHttp {

    private final ProductQueryUseCase queryUseCase;

    @GetMapping("/tenant/passports/{passportId}")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public ApiResponse<PassportDetailResponse> getTenantPassportDetail(
        @CurrentActor ProductActor actor,
        @PathVariable("passportId") String passportId
    ) {
        return ApiResponse.success(PassportDetailResponse.from(queryUseCase.getTenantPassportDetail(actor.tenantId(), passportId)));
    }

    @GetMapping("/passports/{passportId}/state")
    public ApiResponse<AssetStateResponse> getAssetState(
        @PathVariable("passportId") String passportId
    ) {
        return ApiResponse.success(AssetStateResponse.from(queryUseCase.getAssetState(passportId)));
    }

    @GetMapping("/passports/{passportId}/owner")
    public ApiResponse<OwnerResponse> getCurrentOwner(
        @PathVariable("passportId") String passportId
    ) {
        return ApiResponse.success(OwnerResponse.from(queryUseCase.getCurrentOwner(passportId)));
    }

    @GetMapping("/me/passports")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<MyPassportResponse>> listMyPassports(@CurrentActor ProductActor actor) {
        return ApiResponse.success(queryUseCase.listMyPassports(actor.userId()).stream()
            .map(MyPassportResponse::from)
            .toList());
    }

    @GetMapping("/tenant/passports")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public ApiResponse<PagedTenantPassportResponse> listTenantPassports(
        @CurrentActor ProductActor actor,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        @RequestParam(name = "assetState", required = false) String assetState,
        @RequestParam(name = "createdFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
        @RequestParam(name = "createdTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
        @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return ApiResponse.success(PagedTenantPassportResponse.from(
            queryUseCase.listTenantPassports(actor.tenantId(), page, size, assetState, createdFrom, createdTo, keyword)
        ));
    }

    @GetMapping("/tenant/distributed-passports")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public ApiResponse<PagedDistributedPassportResponse> listDistributedPassports(
        @CurrentActor ProductActor actor,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "sourceTenantId", required = false) String sourceTenantId
    ) {
        return ApiResponse.success(PagedDistributedPassportResponse.from(
            queryUseCase.listDistributedPassports(actor.tenantId(), page, size, keyword, sourceTenantId)
        ));
    }

    @GetMapping("/tenant/distributed-passports/{passportId}")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public ApiResponse<DistributedPassportDetailResponse> getDistributedPassportDetail(
        @CurrentActor ProductActor actor,
        @PathVariable("passportId") String passportId
    ) {
        return ApiResponse.success(DistributedPassportDetailResponse.from(
            queryUseCase.getDistributedPassportDetail(actor.tenantId(), passportId)
        ));
    }

    @GetMapping("/tenant/completed-transfers/{passportId}")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public ApiResponse<DistributedPassportDetailResponse> getCompletedTransferDetail(
        @CurrentActor ProductActor actor,
        @PathVariable("passportId") String passportId
    ) {
        return ApiResponse.success(DistributedPassportDetailResponse.from(
            queryUseCase.getCompletedTransferDetail(actor.tenantId(), passportId)
        ));
    }
}
