package io.attestry.product.interfaces.http;

import io.attestry.product.application.usecase.ProductQueryUseCase;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.security.CurrentActor;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductQueryHttp {

    private final ProductQueryUseCase queryUseCase;

    public ProductQueryHttp(ProductQueryUseCase queryUseCase) {
        this.queryUseCase = queryUseCase;
    }

    @GetMapping("/tenant/passports/{passportId}")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public PassportDetailResponse getTenantPassportDetail(
        @CurrentActor ActorContext actor,
        @PathVariable("passportId") String passportId
    ) {
        ProductQueryUseCase.PassportDetailResponse result = queryUseCase.getTenantPassportDetail(actor.tenantId(),
            passportId);
        return new PassportDetailResponse(
            result.passportId(), result.qrPublicCode(),
            result.tenantId(),
            result.assetId(), result.serialNumber(), result.modelId(), result.modelName(),
            result.manufacturedAt(), result.productionBatch(), result.factoryCode(),
            result.assetState(), result.riskFlag(),
            result.ownerId(), result.ownerUpdatedAt(), result.createdAt()
        );
    }

    @GetMapping("/passports/{passportId}/state")
    public AssetStateResponse getAssetState(
        @PathVariable("passportId") String passportId
    ) {
        ProductQueryUseCase.AssetStateResponse result = queryUseCase.getAssetState(passportId);
        return new AssetStateResponse(result.assetId(), result.passportId(), result.assetState(), result.riskFlag());
    }

    @GetMapping("/passports/{passportId}/owner")
    public OwnerResponse getCurrentOwner(
        @PathVariable("passportId") String passportId
    ) {
        ProductQueryUseCase.OwnerResponse result = queryUseCase.getCurrentOwner(passportId);
        return new OwnerResponse(result.passportId(), result.ownerId(), result.updatedAt());
    }

    @GetMapping("/me/passports")
    @PreAuthorize("isAuthenticated()")
    public List<MyPassportResponse> listMyPassports(@CurrentActor ActorContext actor) {
        return queryUseCase.listMyPassports(actor.userId()).stream()
            .map(r -> new MyPassportResponse(
                r.passportId(), r.qrPublicCode(),
                r.tenantId(),
                r.assetId(), r.serialNumber(), r.modelName(),
                r.assetState(), r.riskFlag(), r.ownedSince()
            ))
            .toList();
    }

    @GetMapping("/tenant/passports")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public PagedTenantPassportResponse listTenantPassports(
        @CurrentActor ActorContext actor,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        ProductQueryUseCase.PagedTenantPassportResponse result = queryUseCase.listTenantPassports(actor.tenantId(),
            page, size);
        List<TenantPassportResponse> content = result.content().stream()
            .map(r -> new TenantPassportResponse(
                r.passportId(), r.serialNumber(), r.modelId(), r.modelName(),
                r.assetState(), r.createdAt()
            ))
            .toList();
        return new PagedTenantPassportResponse(content, result.page(), result.size(), result.totalElements(),
            result.totalPages());
    }

    public record MyPassportResponse(
        String passportId,
        String qrPublicCode,
        String tenantId,
        String assetId,
        String serialNumber,
        String modelName,
        String assetState,
        String riskFlag,
        Instant ownedSince
    ) {
    }

    public record PassportDetailResponse(
        String passportId,
        String qrPublicCode,
        String tenantId,
        String assetId,
        String serialNumber,
        String modelId,
        String modelName,
        Instant manufacturedAt,
        String productionBatch,
        String factoryCode,
        String assetState,
        String riskFlag,
        String ownerId,
        Instant ownerUpdatedAt,
        Instant createdAt
    ) {
    }

    public record AssetStateResponse(String assetId, String passportId, String assetState, String riskFlag) {
    }

    public record OwnerResponse(String passportId, String ownerId, Instant updatedAt) {
    }

    public record TenantPassportResponse(
        String passportId,
        String serialNumber,
        String modelId,
        String modelName,
        String assetState,
        Instant createdAt
    ) {
    }

    public record PagedTenantPassportResponse(
        List<TenantPassportResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }
}
