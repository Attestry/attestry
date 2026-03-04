package io.attestry.product.interfaces.http;

import io.attestry.product.application.usecase.PassportPermissionUseCase;
import io.attestry.product.domain.permission.model.PermissionScope;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.security.CurrentActor;
import java.time.Instant;
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
    public GrantResponse grantPermission(
        @CurrentActor ActorContext actor,
        @PathVariable("passportId") String passportId,
        @RequestBody GrantRequest request
    ) {
        PassportPermissionUseCase.GrantResult result = permissionUseCase.grantPermission(
            actor,
            new PassportPermissionUseCase.GrantCommand(
                passportId,
                request.sellerGroupId(),
                request.scope(),
                request.expiresAt()
            )
        );
        return new GrantResponse(result.permissionId(), result.passportId(), result.sellerGroupId(), result.scope());
    }

    @DeleteMapping("/passports/{passportId}/permissions/{permissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokePermission(
        @CurrentActor ActorContext actor,
        @PathVariable("passportId") String passportId,
        @PathVariable("permissionId") String permissionId
    ) {
        permissionUseCase.revokePermission(actor, permissionId);
    }

    @PatchMapping("/passports/{passportId}/permissions/{permissionId}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspendPermission(
        @CurrentActor ActorContext actor,
        @PathVariable("passportId") String passportId,
        @PathVariable("permissionId") String permissionId
    ) {
        permissionUseCase.suspendPermission(actor, permissionId);
    }

    public record GrantRequest(String sellerGroupId, PermissionScope scope, Instant expiresAt) {
    }

    public record GrantResponse(String permissionId, String passportId, String sellerGroupId, String scope) {
    }
}
