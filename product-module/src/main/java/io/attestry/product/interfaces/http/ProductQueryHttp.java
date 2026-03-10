package io.attestry.product.interfaces.http;

import io.attestry.commonlib.web.CurrentActor;
import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.usecase.ProductQueryUseCase;
import io.attestry.product.interfaces.http.dto.response.AssetStateResponse;
import io.attestry.product.interfaces.http.dto.response.DistributedPassportDetailResponse;
import io.attestry.product.interfaces.http.dto.response.MyPassportResponse;
import io.attestry.product.interfaces.http.dto.response.OwnerResponse;
import io.attestry.product.interfaces.http.dto.response.PagedDistributedPassportResponse;
import io.attestry.product.interfaces.http.dto.response.PagedTenantPassportResponse;
import io.attestry.product.interfaces.http.dto.response.PassportDetailResponse;
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
    public PassportDetailResponse getTenantPassportDetail(
        @CurrentActor ProductActor actor,
        @PathVariable("passportId") String passportId
    ) {
        return PassportDetailResponse.from(queryUseCase.getTenantPassportDetail(actor.tenantId(), passportId));
    }

    @GetMapping("/passports/{passportId}/state")
    public AssetStateResponse getAssetState(
        @PathVariable("passportId") String passportId
    ) {
        return AssetStateResponse.from(queryUseCase.getAssetState(passportId));
    }

    @GetMapping("/passports/{passportId}/owner")
    public OwnerResponse getCurrentOwner(
        @PathVariable("passportId") String passportId
    ) {
        return OwnerResponse.from(queryUseCase.getCurrentOwner(passportId));
    }

    @GetMapping("/me/passports")
    @PreAuthorize("isAuthenticated()")
    public List<MyPassportResponse> listMyPassports(@CurrentActor ProductActor actor) {
        return queryUseCase.listMyPassports(actor.userId()).stream()
            .map(MyPassportResponse::from)
            .toList();
    }

    @GetMapping("/tenant/passports")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public PagedTenantPassportResponse listTenantPassports(
        @CurrentActor ProductActor actor,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        @RequestParam(name = "assetState", required = false) String assetState,
        @RequestParam(name = "createdFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
        @RequestParam(name = "createdTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
        @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return PagedTenantPassportResponse.from(
            queryUseCase.listTenantPassports(actor.tenantId(), page, size, assetState, createdFrom, createdTo, keyword)
        );
    }

    @GetMapping("/tenant/distributed-passports")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public PagedDistributedPassportResponse listDistributedPassports(
        @CurrentActor ProductActor actor,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "sourceTenantId", required = false) String sourceTenantId
    ) {
        return PagedDistributedPassportResponse.from(
            queryUseCase.listDistributedPassports(actor.tenantId(), page, size, keyword, sourceTenantId)
        );
    }

    @GetMapping("/tenant/distributed-passports/{passportId}")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public DistributedPassportDetailResponse getDistributedPassportDetail(
        @CurrentActor ProductActor actor,
        @PathVariable("passportId") String passportId
    ) {
        return DistributedPassportDetailResponse.from(
            queryUseCase.getDistributedPassportDetail(actor.tenantId(), passportId)
        );
    }
}
