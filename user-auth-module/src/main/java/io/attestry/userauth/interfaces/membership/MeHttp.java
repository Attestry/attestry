package io.attestry.userauth.interfaces.membership;

import io.attestry.userauth.application.dto.MembershipView;
import io.attestry.userauth.application.membership.MembershipQueryService;
import io.attestry.userauth.security.AuthPrincipalResolver;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class MeHttp {

    private final MembershipQueryService membershipQueryService;

    public MeHttp(MembershipQueryService membershipQueryService) {
        this.membershipQueryService = membershipQueryService;
    }

    @GetMapping("/memberships")
    public List<MembershipView> memberships(Authentication authentication) {
        return membershipQueryService.getMemberships(AuthPrincipalResolver.resolve(authentication).userId());
    }
}
