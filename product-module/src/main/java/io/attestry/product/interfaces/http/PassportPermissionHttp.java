package io.attestry.product.interfaces.http;

import io.attestry.commonlib.web.CurrentActor;
import io.attestry.product.application.dto.command.GrantCommand;
import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.usecase.PassportPermissionUseCase;
import io.attestry.product.interfaces.http.dto.request.GrantPermissionRequest;
import io.attestry.product.interfaces.http.dto.response.GrantPermissionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class PassportPermissionHttp {

    private final PassportPermissionUseCase permissionUseCase;

    public PassportPermissionHttp(PassportPermissionUseCase permissionUseCase) {
        this.permissionUseCase = permissionUseCase;
    }

    @PostMapping("/passports/{passportId}/permissions")
    @ResponseStatus(HttpStatus.CREATED)
    public GrantPermissionResponse grantPermission(
        @CurrentActor ProductActor actor,
        @PathVariable("passportId") String passportId,
        @RequestBody GrantPermissionRequest request
    ) {
        return GrantPermissionResponse.from(permissionUseCase.grantPermission(
            actor,
            new GrantCommand(
                passportId,
                request.sellerTenantId(),
                request.scope(),
                request.expiresAt()
            )
        ));
    }

    @DeleteMapping("/passports/{passportId}/permissions/{permissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokePermission(
        @CurrentActor ProductActor actor,
        @PathVariable("passportId") String passportId,
        @PathVariable("permissionId") String permissionId
    ) {
        permissionUseCase.revokePermission(actor, permissionId);
    }

    @PatchMapping("/passports/{passportId}/permissions/{permissionId}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspendPermission(
        @CurrentActor ProductActor actor,
        @PathVariable("passportId") String passportId,
        @PathVariable("permissionId") String permissionId
    ) {
        permissionUseCase.suspendPermission(actor, permissionId);
    }
}
