package io.attestry.userauth.interfaces.membership;

import io.attestry.userauth.application.dto.result.MembershipInvitationResult;
import io.attestry.userauth.application.dto.result.MembershipResult;
import io.attestry.userauth.application.dto.result.MembershipAssignableRolesResult;
import io.attestry.userauth.application.dto.result.MembershipPermissionTemplateResult;
import io.attestry.userauth.application.dto.result.MembershipRoleAssignmentsResult;
import io.attestry.userauth.application.dto.result.TenantAvailableTemplateCodesResult;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.usecase.membership.MembershipAdminUseCase;
import io.attestry.userauth.security.CurrentActor;
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
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @RequestBody InviteRequest request
    ) {

        MembershipInvitationResult result = membershipAdminService.invite(
            actor,
            tenantId,
            new MembershipAdminUseCase.InviteCommand(request.email(), request.groupId(), request.role())
        );
        return InvitationResponse.from(result);
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public MembershipResponse acceptInvitation(@CurrentActor ActorContext actor, @PathVariable("invitationId") String invitationId) {
        MembershipResult result = membershipAdminService.acceptInvitation(actor, invitationId);
        return MembershipResponse.from(result);
    }

    @GetMapping("/tenants/{tenantId}/admin/memberships")
    @PreAuthorize("hasAnyAuthority('SCOPE_TENANT_MEMBERSHIP_VIEW','SCOPE_TENANT_READ_ONLY')")
    public List<MembershipResponse> listMemberships(@CurrentActor ActorContext actor, @PathVariable("tenantId") String tenantId) {
        return membershipAdminService.listMemberships(actor, tenantId)
            .stream()
            .map(MembershipResponse::from)
            .toList();
    }

    @PatchMapping("/tenants/{tenantId}/admin/memberships/{id}/status")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_MEMBERSHIP_ENFORCE')")
    public MembershipResponse updateMembershipStatus(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String membershipId,
        @RequestBody UpdateMembershipStatusRequest request
    ) {
        MembershipResult result = membershipAdminService.updateMembershipStatus(
            actor,
            tenantId,
            membershipId,
            new MembershipAdminUseCase.UpdateMembershipStatusCommand(request.status())
        );
        return MembershipResponse.from(result);
    }

    @GetMapping("/tenants/{tenantId}/admin/memberships/{id}/roles")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipRoleAssignmentsResponse listMembershipRoles(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String membershipId
    ) {
        MembershipRoleAssignmentsResult result = membershipAdminService.listMembershipRoleAssignments(actor, tenantId, membershipId);
        return MembershipRoleAssignmentsResponse.from(result);
    }

    @GetMapping("/tenants/{tenantId}/admin/memberships/{id}/roles/available")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipAssignableRolesResponse listAssignableRoleCodes(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String membershipId
    ) {
        MembershipAssignableRolesResult result = membershipAdminService.listAssignableRoleCodes(actor, tenantId, membershipId);
        return MembershipAssignableRolesResponse.from(result);
    }

    @GetMapping("/tenants/{tenantId}/admin/permission-templates/available")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public TenantAvailableTemplateCodesResponse listTenantAvailableTemplateCodes(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId
    ) {
        TenantAvailableTemplateCodesResult result = membershipAdminService.listTenantAvailableTemplateCodes(
            actor,
            tenantId
        );
        return TenantAvailableTemplateCodesResponse.from(result);
    }

    @PostMapping("/tenants/{tenantId}/admin/memberships/{id}/roles/{roleCode}")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipRoleAssignmentsResponse assignMembershipRole(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String membershipId,
        @PathVariable("roleCode") String roleCode
    ) {
        MembershipRoleAssignmentsResult result = membershipAdminService.assignMembershipRole(
            actor,
            tenantId,
            membershipId,
            new MembershipAdminUseCase.AssignMembershipRoleCommand(roleCode)
        );
        return MembershipRoleAssignmentsResponse.from(result);
    }

    @DeleteMapping("/tenants/{tenantId}/admin/memberships/{id}/roles/{roleCode}")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipRoleAssignmentsResponse revokeMembershipRole(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String membershipId,
        @PathVariable("roleCode") String roleCode
    ) {
        MembershipRoleAssignmentsResult result = membershipAdminService.revokeMembershipRole(
            actor,
            tenantId,
            membershipId,
            new MembershipAdminUseCase.RevokeMembershipRoleCommand(roleCode)
        );
        return MembershipRoleAssignmentsResponse.from(result);
    }

    @PostMapping("/tenants/{tenantId}/admin/memberships/{id}/permission-templates/{templateCode}/apply")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipPermissionTemplateResponse applyPermissionTemplate(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String membershipId,
        @PathVariable("templateCode") String templateCode,
        @RequestBody(required = false) MutatePermissionTemplateRequest request
    ) {
        MembershipPermissionTemplateResult result = membershipAdminService.applyPermissionTemplate(
            actor,
            tenantId,
            membershipId,
            new MembershipAdminUseCase.ApplyPermissionTemplateCommand(templateCode, request == null ? null : request.reason())
        );
        return MembershipPermissionTemplateResponse.from(result);
    }

    @PostMapping("/tenants/{tenantId}/admin/memberships/{id}/permission-templates/{templateCode}/revoke")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipPermissionTemplateResponse revokePermissionTemplate(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String membershipId,
        @PathVariable("templateCode") String templateCode,
        @RequestBody(required = false) MutatePermissionTemplateRequest request
    ) {
        MembershipPermissionTemplateResult result = membershipAdminService.revokePermissionTemplate(
            actor,
            tenantId,
            membershipId,
            new MembershipAdminUseCase.RevokePermissionTemplateCommand(templateCode, request == null ? null : request.reason())
        );
        return MembershipPermissionTemplateResponse.from(result);
    }
}
