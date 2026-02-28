package io.attestry.userauth.interfaces.membership;

import io.attestry.userauth.application.dto.view.MembershipView;
import io.attestry.userauth.application.usecase.membership.MembershipQueryUseCase;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class MeHttp {

    private final MembershipQueryUseCase membershipQueryService;

    public MeHttp(MembershipQueryUseCase membershipQueryService) {
        this.membershipQueryService = membershipQueryService;
    }

    @GetMapping("/memberships")
    public List<MembershipView> memberships(@AuthenticationPrincipal AuthPrincipal principal) {
        return membershipQueryService.getMemberships(principal.userId());
    }
}
