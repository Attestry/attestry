package io.attestry.userauth.interfaces.membership;

import io.attestry.userauth.application.membership.MembershipAdminService;
import io.attestry.userauth.domain.membership.model.Invitation;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.security.AuthPrincipalResolver;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class MembershipAdminHttp {

    private final MembershipAdminService membershipAdminService;

    public MembershipAdminHttp(MembershipAdminService membershipAdminService) {
        this.membershipAdminService = membershipAdminService;
    }

    @PostMapping("/tenants/{tenantId}/admin/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_MEMBERSHIP_MANAGE')")
    public InvitationResponse invite(
        Authentication authentication,
        @PathVariable("tenantId") String tenantId,
        @RequestBody InviteRequest request
    ) {
        Invitation invitation = membershipAdminService.invite(
            AuthPrincipalResolver.resolve(authentication),
            tenantId,
            new MembershipAdminService.InviteCommand(request.email(), request.groupId(), request.role())
        );
        return InvitationResponse.from(invitation);
    }

    @PostMapping("/invitations/{id}/accept")
    public MembershipResponse acceptInvitation(Authentication authentication, @PathVariable("id") String id) {
        Membership membership = membershipAdminService.acceptInvitation(AuthPrincipalResolver.resolve(authentication), id);
        return MembershipResponse.from(membership);
    }

    @GetMapping("/tenants/{tenantId}/admin/memberships")
    @PreAuthorize("hasAuthority('SCOPE_MEMBERSHIP_MANAGE')")
    public List<MembershipResponse> listMemberships(Authentication authentication, @PathVariable("tenantId") String tenantId) {
        return membershipAdminService.listMemberships(AuthPrincipalResolver.resolve(authentication), tenantId)
            .stream()
            .map(MembershipResponse::from)
            .toList();
    }

    @PatchMapping("/tenants/{tenantId}/admin/memberships/{id}")
    @PreAuthorize("hasAuthority('SCOPE_MEMBERSHIP_MANAGE')")
    public MembershipResponse updateMembership(
        Authentication authentication,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String membershipId,
        @RequestBody UpdateMembershipRequest request
    ) {
        Membership updated = membershipAdminService.updateMembership(
            AuthPrincipalResolver.resolve(authentication),
            tenantId,
            membershipId,
            new MembershipAdminService.UpdateMembershipCommand(request.role(), request.status())
        );
        return MembershipResponse.from(updated);
    }

    public record InviteRequest(String email, String groupId, MembershipRole role) {
    }

    public record UpdateMembershipRequest(MembershipRole role, MembershipStatus status) {
    }

    public record InvitationResponse(
        String invitationId,
        String tenantId,
        String groupId,
        String inviteeEmail,
        String role,
        String status
    ) {
        static InvitationResponse from(Invitation invitation) {
            return new InvitationResponse(
                invitation.invitationId(),
                invitation.tenantId(),
                invitation.groupId(),
                invitation.inviteeEmail().value(),
                invitation.role().name(),
                invitation.status().name()
            );
        }
    }

    public record MembershipResponse(
        String membershipId,
        String tenantId,
        String groupId,
        String role,
        String status
    ) {
        static MembershipResponse from(Membership membership) {
            return new MembershipResponse(
                membership.membershipId(),
                membership.tenantId(),
                membership.groupId(),
                membership.role().name(),
                membership.status().name()
            );
        }
    }
}
