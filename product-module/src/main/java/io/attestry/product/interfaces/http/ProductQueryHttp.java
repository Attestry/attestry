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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductQueryHttp {

    private final ProductQueryUseCase queryUseCase;

    public ProductQueryHttp(ProductQueryUseCase queryUseCase) {
        this.queryUseCase = queryUseCase;
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
                r.tenantId(), r.groupId(),
                r.assetId(), r.serialNumber(), r.modelName(),
                r.assetState(), r.riskFlag(), r.ownedSince()
            ))
            .toList();
    }

    public record MyPassportResponse(
        String passportId,
        String qrPublicCode,
        String tenantId,
        String groupId,
        String assetId,
        String serialNumber,
        String modelName,
        String assetState,
        String riskFlag,
        Instant ownedSince
    ) {
    }

    public record AssetStateResponse(String assetId, String passportId, String assetState, String riskFlag) {
    }

    public record OwnerResponse(String passportId, String ownerId, Instant updatedAt) {
    }
}
