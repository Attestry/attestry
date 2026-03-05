package io.attestry.product.interfaces.http;

import io.attestry.product.application.usecase.ProductMintUseCase;
import io.attestry.product.application.usecase.ProductQueryUseCase;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.security.CurrentActor;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductMintHttp {

    private final ProductMintUseCase mintUseCase;
    private final ProductQueryUseCase queryUseCase;

    public ProductMintHttp(ProductMintUseCase mintUseCase, ProductQueryUseCase queryUseCase) {
        this.mintUseCase = mintUseCase;
        this.queryUseCase = queryUseCase;
    }

    @PostMapping("/tenants/{tenantId}/groups/{groupId}/minted")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_BRAND_MINT')")
    public MintedProductResponse mint(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("groupId") String groupId,
        @RequestBody MintedProductRequest request
    ) {
        ProductMintUseCase.MintedProductResult result = mintUseCase.mint(
            actor,
            new ProductMintUseCase.MintProductCommand(
                tenantId,
                groupId,
                request.serialNumber(),
                request.modelId(),
                request.modelName(),
                request.manufacturedAt(),
                request.productionBatch(),
                request.factoryCode(),
                request.componentRootHash()
            )
        );
        return MintedProductResponse.from(result);
    }

    @GetMapping("/tenants/{tenantId}/groups/{groupId}/minted")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public List<MintedPassportListResponse> listMinted(
        @PathVariable("tenantId") String tenantId,
        @PathVariable("groupId") String groupId
    ) {
        return queryUseCase.listMintedPassports(tenantId, groupId).stream()
            .map(r -> new MintedPassportListResponse(
                r.passportId(), r.qrPublicCode(),
                r.assetId(), r.serialNumber(), r.modelId(), r.modelName(),
                r.manufacturedAt(), r.assetState(), r.riskFlag(),
                r.ownerId(), r.createdAt()
            ))
            .toList();
    }

    public record MintedPassportListResponse(
        String passportId,
        String qrPublicCode,
        String assetId,
        String serialNumber,
        String modelId,
        String modelName,
        Instant manufacturedAt,
        String assetState,
        String riskFlag,
        String ownerId,
        Instant createdAt
    ) {
    }

    public record MintedProductRequest(
        String serialNumber,
        String modelId,
        String modelName,
        Instant manufacturedAt,
        String productionBatch,
        String factoryCode,
        String componentRootHash
    ) {
    }

    public record MintedProductResponse(
        String assetId,
        String passportId,
        String qrPublicCode,
        String outboxEventId,
        String ledgerEventCategory,
        String ledgerEventAction
    ) {
        static MintedProductResponse from(ProductMintUseCase.MintedProductResult result) {
            return new MintedProductResponse(
                result.assetId(),
                result.passportId(),
                result.qrPublicCode(),
                result.outboxEventId(),
                result.ledgerEventCategory(),
                result.ledgerEventAction()
            );
        }
    }
}
