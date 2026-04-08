package io.attestry.product.interfaces.http.command;

import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.commonlib.web.CurrentActor;
import io.attestry.product.application.common.ProductActor;
import io.attestry.product.application.command.model.RetireCommand;
import io.attestry.product.application.command.ProductRetireUseCase;
import io.attestry.product.interfaces.http.command.dto.response.RetireResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductRetireHttp {

    private final ProductRetireUseCase retireUseCase;

    @PostMapping("/passports/{passportId}/retire")
    @PreAuthorize("hasAuthority('SCOPE_OWNER_RETIRE')")
    public ApiResponse<RetireResponse> retire(
        @CurrentActor ProductActor actor,
        @PathVariable("passportId") String passportId
    ) {
        return ApiResponse.success(RetireResponse.from(
            retireUseCase.retire(actor, new RetireCommand(passportId))
        ));
    }
}
