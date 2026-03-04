package io.attestry.product.interfaces.http;

import io.attestry.product.application.usecase.ProductVoidUseCase;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.security.CurrentActor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@PreAuthorize("hasAuthority('SCOPE_BRAND_VOID')")
public class ProductVoidHttp {

    private final ProductVoidUseCase voidUseCase;

    public ProductVoidHttp(ProductVoidUseCase voidUseCase) {
        this.voidUseCase = voidUseCase;
    }

    @PostMapping("/tenants/{tenantId}/groups/{groupId}/passports/{passportId}/void")
    public VoidResponse voidAsset(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("groupId") String groupId,
        @PathVariable("passportId") String passportId,
        @RequestBody VoidRequest request
    ) {
        ProductVoidUseCase.VoidResult result = voidUseCase.voidAsset(
            actor,
            new ProductVoidUseCase.VoidCommand(tenantId, groupId, passportId, request.reason(), request.note())
        );
        return new VoidResponse(result.assetId(), result.assetState(), result.outboxEventId());
    }

    public record VoidRequest(String reason, String note) {
    }

    public record VoidResponse(String assetId, String assetState, String outboxEventId) {
    }
}
