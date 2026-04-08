package io.attestry.product.interfaces.http.command;

import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.commonlib.web.CurrentActor;
import io.attestry.product.application.command.model.GrantCommand;
import io.attestry.product.application.common.ProductActor;
import io.attestry.product.application.command.PassportPermissionUseCase;
import io.attestry.product.interfaces.http.command.dto.request.GrantPermissionRequest;
import io.attestry.product.interfaces.http.command.dto.response.GrantPermissionResponse;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class PassportPermissionHttp {

    private final PassportPermissionUseCase permissionUseCase;

    @PostMapping("/passports/{passportId}/permissions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GrantPermissionResponse> grantPermission(
        @CurrentActor ProductActor actor,
        @PathVariable("passportId") String passportId,
        @RequestBody GrantPermissionRequest request
    ) {
        return ApiResponse.success(GrantPermissionResponse.from(permissionUseCase.grantPermission(
            actor,
            new GrantCommand(
                passportId,
                request.sellerTenantId(),
                request.scope(),
                request.expiresAt()
            )
        )));
    }

    @DeleteMapping("/passports/{passportId}/permissions/{permissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> revokePermission(
        @CurrentActor ProductActor actor,
        @PathVariable("passportId") String passportId,
        @PathVariable("permissionId") String permissionId
    ) {
        permissionUseCase.revokePermission(actor, permissionId);
        return ApiResponse.success();
    }

    @PatchMapping("/passports/{passportId}/permissions/{permissionId}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> suspendPermission(
        @CurrentActor ProductActor actor,
        @PathVariable("passportId") String passportId,
        @PathVariable("permissionId") String permissionId
    ) {
        permissionUseCase.suspendPermission(actor, permissionId);
        return ApiResponse.success();
    }
}
