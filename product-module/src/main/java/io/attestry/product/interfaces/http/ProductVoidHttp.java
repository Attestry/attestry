package io.attestry.product.interfaces.http;

import io.attestry.commonlib.web.CurrentActor;
import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.dto.command.VoidCommand;
import io.attestry.product.application.usecase.ProductVoidUseCase;
import io.attestry.product.interfaces.http.dto.request.VoidRequest;
import io.attestry.product.interfaces.http.dto.response.VoidResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/products")
@PreAuthorize("hasAuthority('SCOPE_BRAND_VOID')")
public class ProductVoidHttp {

    private final ProductVoidUseCase voidUseCase;

    @PostMapping("/passports/{passportId}/void")
    public VoidResponse voidAsset(
        @CurrentActor ProductActor actor,
        @PathVariable("passportId") String passportId,
        @RequestBody VoidRequest request
    ) {
        return VoidResponse.from(voidUseCase.voidAsset(
            actor,
            new VoidCommand(actor.tenantId(), passportId, request.reason(), request.note())
        ));
    }
}
