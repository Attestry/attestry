package io.attestry.userauth.interfaces.membership;

import io.attestry.commonlib.web.CurrentActor;
import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.application.auth.command.UpdateMyAccountCommand;
import io.attestry.userauth.application.auth.view.MyAccountView;
import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.auth.usecase.MyAccountQueryUseCase;
import io.attestry.userauth.application.membership.usecase.MembershipQueryUseCase;
import io.attestry.userauth.application.membership.view.MembershipView;
import io.attestry.userauth.interfaces.membership.dto.request.UpdateMyAccountRequest;
import java.util.List;

import jakarta.validation.Valid;
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

    //TODO("DELETE /me/account 추가 ")
//  2. 내부에서 UserStatus -> SUSPENDED(소프트 삭제)
//  3. 현재 액세스 토큰 revoke
//  4. 이후 로그인 차단(checkActiveStatus 이미 있음)
//  5. 필요하면 membership도 함께 비활성 처리

}
