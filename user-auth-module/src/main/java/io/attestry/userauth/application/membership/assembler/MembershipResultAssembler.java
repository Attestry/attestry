package io.attestry.userauth.application.membership.assembler;

import io.attestry.userauth.application.dto.result.InvitationResult;
import io.attestry.userauth.application.dto.result.MembershipResult;
import io.attestry.userauth.application.port.identity.UserAccountRepositoryPort;
import io.attestry.userauth.domain.membership.model.Invitation;
import io.attestry.userauth.domain.membership.model.Membership;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipResultAssembler {

    private final UserAccountRepositoryPort userAccountRepository;

    public InvitationResult toInvitationResult(Invitation invitation) {
        return new InvitationResult(
            invitation.invitationId(),
            invitation.tenantId(),
            invitation.inviteeEmail().value(),
            invitation.role().name(),
            invitation.status().name()
        );
    }

    public MembershipResult toMembershipResult(Membership membership) {
        String email = userAccountRepository.findById(membership.userId())
            .map(user -> user.email().value())
            .orElse(null);
        return new MembershipResult(
            membership.membershipId(),
            membership.tenantId(),
            email,
            membership.currentRoleCodes().stream().sorted().toList(),
            membership.status().name()
        );
    }
}
