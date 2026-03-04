package io.attestry.product.interfaces.http;

import io.attestry.product.application.usecase.ProductMintUseCase;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.security.CurrentActor;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@PreAuthorize("hasAuthority('SCOPE_BRAND_MINT')")
public class ProductMintHttp {

    private final ProductMintUseCase mintUseCase;

    public ProductMintHttp(ProductMintUseCase mintUseCase) {
        this.mintUseCase = mintUseCase;
    }

    @PostMapping("/tenants/{tenantId}/groups/{groupId}/minted")
    @ResponseStatus(HttpStatus.CREATED)
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
