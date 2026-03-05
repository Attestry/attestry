package io.attestry.userauth.interfaces.membership;

import io.attestry.userauth.application.dto.command.UpdateMyAccountCommand;
import io.attestry.userauth.application.dto.view.MembershipView;
import io.attestry.userauth.application.dto.view.MyAccountView;
import io.attestry.userauth.application.usecase.auth.MyAccountQueryUseCase;
import io.attestry.userauth.application.usecase.membership.MembershipQueryUseCase;
import io.attestry.userauth.security.AuthPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class MeHttp {

    private final MembershipQueryUseCase membershipQueryService;
    private final MyAccountQueryUseCase myAccountQueryUseCase;

    public MeHttp(
        MembershipQueryUseCase membershipQueryService,
        MyAccountQueryUseCase myAccountQueryUseCase
    ) {
        this.membershipQueryService = membershipQueryService;
        this.myAccountQueryUseCase = myAccountQueryUseCase;
    }

    @GetMapping("/account")
    public MyAccountView myAccount(@AuthenticationPrincipal AuthPrincipal principal) {
        return myAccountQueryUseCase.getMyAccount(principal.userId());
    }

    @PatchMapping("/account")
    public MyAccountView updateMyAccount(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody UpdateMyAccountCommand command
    ) {
        return myAccountQueryUseCase.updateMyAccount(principal.userId(), command);
    }

    @GetMapping("/memberships")
    public List<MembershipView> memberships(@AuthenticationPrincipal AuthPrincipal principal) {
        return membershipQueryService.getMemberships(principal.userId());
    }
}
