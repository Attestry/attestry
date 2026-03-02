package io.attestry.userauth.interfaces.membership;

import io.attestry.userauth.application.dto.result.MembershipInvitationResult;
import io.attestry.userauth.application.dto.result.MembershipResult;
import io.attestry.userauth.application.dto.result.MembershipAssignableRolesResult;
import io.attestry.userauth.application.dto.result.MembershipPermissionTemplateResult;
import io.attestry.userauth.application.dto.result.MembershipRoleAssignmentsResult;
import io.attestry.userauth.application.dto.result.TenantAvailableTemplateCodesResult;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.view.MembershipAdminView;
import io.attestry.userauth.application.usecase.membership.MembershipAdminUseCase;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import java.util.List;

import io.attestry.userauth.interfaces.membership.dto.request.InviteRequest;
import io.attestry.userauth.interfaces.membership.dto.request.MutatePermissionTemplateRequest;
import io.attestry.userauth.interfaces.membership.dto.request.UpdateMembershipStatusRequest;
import io.attestry.userauth.interfaces.membership.dto.response.InvitationResponse;
import io.attestry.userauth.interfaces.membership.dto.response.MembershipAssignableRolesResponse;
import io.attestry.userauth.interfaces.membership.dto.response.MembershipPermissionTemplateResponse;
import io.attestry.userauth.interfaces.membership.dto.response.MembershipResponse;
import io.attestry.userauth.interfaces.membership.dto.response.MembershipRoleAssignmentsResponse;
import io.attestry.userauth.interfaces.membership.dto.response.TenantAvailableTemplateCodesResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    private final MembershipAdminUseCase membershipAdminService;

    public MembershipAdminHttp(MembershipAdminUseCase membershipAdminService) {
        this.membershipAdminService = membershipAdminService;
    }

    @PostMapping("/tenants/{tenantId}/admin/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_TENANT_INVITATION_CREATE')")
    public InvitationResponse invite(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @RequestBody InviteRequest request
    ) {

        MembershipInvitationResult result = membershipAdminService.invite(
            toActorContext(principal),
            tenantId,
            new MembershipAdminUseCase.InviteCommand(request.email(), request.groupId(), request.role())
        );
        return InvitationResponse.from(result);
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public MembershipResponse acceptInvitation(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable("invitationId") String invitationId) {
        MembershipResult result = membershipAdminService.acceptInvitation(toActorContext(principal), invitationId);
        return MembershipResponse.from(result);
    }

    @GetMapping("/tenants/{tenantId}/admin/memberships")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_MEMBERSHIP_VIEW')")
    public List<MembershipResponse> listMemberships(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable("tenantId") String tenantId) {
        return membershipAdminService.listMemberships(toActorContext(principal), tenantId)
            .stream()
            .map(MembershipResponse::from)
            .toList();
    }

    @PatchMapping("/tenants/{tenantId}/admin/memberships/{id}/status")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_MEMBERSHIP_ENFORCE')")
    public MembershipResponse updateMembershipStatus(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String membershipId,
        @RequestBody UpdateMembershipStatusRequest request
    ) {
        MembershipResult result = membershipAdminService.updateMembershipStatus(
            toActorContext(principal),
            tenantId,
            membershipId,
            new MembershipAdminUseCase.UpdateMembershipStatusCommand(request.status())
        );
        return MembershipResponse.from(result);
    }

    @GetMapping("/tenants/{tenantId}/admin/memberships/{id}/roles")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipRoleAssignmentsResponse listMembershipRoles(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String membershipId
    ) {
        MembershipRoleAssignmentsResult result = membershipAdminService.listMembershipRoleAssignments(toActorContext(principal), tenantId, membershipId);
        return MembershipRoleAssignmentsResponse.from(result);
    }

    @GetMapping("/tenants/{tenantId}/admin/memberships/{id}/roles/available")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipAssignableRolesResponse listAssignableRoleCodes(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String membershipId
    ) {
        MembershipAssignableRolesResult result = membershipAdminService.listAssignableRoleCodes(toActorContext(principal), tenantId, membershipId);
        return MembershipAssignableRolesResponse.from(result);
    }

    @GetMapping("/tenants/{tenantId}/admin/permission-templates/available")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public TenantAvailableTemplateCodesResponse listTenantAvailableTemplateCodes(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId
    ) {
        TenantAvailableTemplateCodesResult result = membershipAdminService.listTenantAvailableTemplateCodes(
            toActorContext(principal),
            tenantId
        );
        return TenantAvailableTemplateCodesResponse.from(result);
    }

    @PostMapping("/tenants/{tenantId}/admin/memberships/{id}/roles/{roleCode}")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipRoleAssignmentsResponse assignMembershipRole(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String membershipId,
        @PathVariable("roleCode") String roleCode
    ) {
        MembershipRoleAssignmentsResult result = membershipAdminService.assignMembershipRole(
            toActorContext(principal),
            tenantId,
            membershipId,
            new MembershipAdminUseCase.AssignMembershipRoleCommand(roleCode)
        );
        return MembershipRoleAssignmentsResponse.from(result);
    }

    @DeleteMapping("/tenants/{tenantId}/admin/memberships/{id}/roles/{roleCode}")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipRoleAssignmentsResponse revokeMembershipRole(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String membershipId,
        @PathVariable("roleCode") String roleCode
    ) {
        MembershipRoleAssignmentsResult result = membershipAdminService.revokeMembershipRole(
            toActorContext(principal),
            tenantId,
            membershipId,
            new MembershipAdminUseCase.RevokeMembershipRoleCommand(roleCode)
        );
        return MembershipRoleAssignmentsResponse.from(result);
    }

    @PostMapping("/tenants/{tenantId}/admin/memberships/{id}/permission-templates/{templateCode}/apply")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipPermissionTemplateResponse applyPermissionTemplate(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String membershipId,
        @PathVariable("templateCode") String templateCode,
        @RequestBody(required = false) MutatePermissionTemplateRequest request
    ) {
        MembershipPermissionTemplateResult result = membershipAdminService.applyPermissionTemplate(
            toActorContext(principal),
            tenantId,
            membershipId,
            new MembershipAdminUseCase.ApplyPermissionTemplateCommand(templateCode, request == null ? null : request.reason())
        );
        return MembershipPermissionTemplateResponse.from(result);
    }

    @PostMapping("/tenants/{tenantId}/admin/memberships/{id}/permission-templates/{templateCode}/revoke")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipPermissionTemplateResponse revokePermissionTemplate(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String membershipId,
        @PathVariable("templateCode") String templateCode,
        @RequestBody(required = false) MutatePermissionTemplateRequest request
    ) {
        MembershipPermissionTemplateResult result = membershipAdminService.revokePermissionTemplate(
            toActorContext(principal),
            tenantId,
            membershipId,
            new MembershipAdminUseCase.RevokePermissionTemplateCommand(templateCode, request == null ? null : request.reason())
        );
        return MembershipPermissionTemplateResponse.from(result);
    }

    private ActorContext toActorContext(AuthPrincipal principal) {
        return new ActorContext(
            principal.tokenId(),
            principal.userId(),
            principal.tenantId(),
            principal.groupId(),
            principal.verificationLevel(),
            principal.scopes(),
            principal.expiresAt()
        );
    }
}
