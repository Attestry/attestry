package io.attestry.userauth.interfaces.membership;

import io.attestry.commonlib.web.CurrentActor;
import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.application.auth.command.UpdateMyAccountCommand;
import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.auth.query.MyAccountQueryUseCase;
import io.attestry.userauth.application.auth.query.MyAccountView;
import io.attestry.userauth.application.membership.query.MembershipQueryUseCase;
import io.attestry.userauth.application.membership.view.MembershipView;
import io.attestry.userauth.interfaces.membership.dto.request.UpdateMyAccountRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/me")
public class MeHttp {

    private final MembershipQueryUseCase membershipQueryService;
    private final MyAccountQueryUseCase myAccountQueryUseCase;


    @GetMapping("/account")
    public ApiResponse<MyAccountView> myAccount(@CurrentActor ActorContext actor) {
        return ApiResponse.success(myAccountQueryUseCase.getMyAccount(actor.userId()));
    }

    @PatchMapping("/account")
    public ApiResponse<MyAccountView> updateMyAccount(
        @CurrentActor ActorContext actor,
        @Valid @RequestBody UpdateMyAccountRequest request
    ) {
        return ApiResponse.success(myAccountQueryUseCase.updateMyAccount(
            actor.userId(),
            new UpdateMyAccountCommand(request.phone(), request.currentPassword(), request.newPassword())
        ));
    }

    @GetMapping("/memberships")
    public ApiResponse<List<MembershipView>> memberships(@CurrentActor ActorContext actor) {
        return ApiResponse.success(membershipQueryService.getMemberships(actor.userId()));
    }

    //TODO("Add DELETE /me/account")
//  2. Internally set UserStatus -> SUSPENDED (soft delete)
//  3. Revoke current access token
//  4. Block subsequent logins (checkActiveStatus already exists)
//  5. Optionally deactivate memberships as well

}
