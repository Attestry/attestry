package io.attestry.userauth.interfaces.membership;

import io.attestry.userauth.application.dto.result.MembershipInvitationResult;
import io.attestry.userauth.application.dto.result.MembershipResult;
import io.attestry.userauth.application.dto.result.MembershipDetailResult;
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
import io.attestry.userauth.interfaces.membership.dto.response.MembershipDetailResponse;
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
@RequestMapping("/admin")
public class MembershipAdminHttp {

    private final MembershipAdminUseCase membershipAdminService;

    public MembershipAdminHttp(MembershipAdminUseCase membershipAdminService) {
        this.membershipAdminService = membershipAdminService;
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_TENANT_INVITATION_CREATE')")
    public InvitationResponse invite(
            @CurrentActor ActorContext actor,
            @RequestBody InviteRequest request) {
        MembershipInvitationResult result = membershipAdminService.invite(
                actor,
                new MembershipAdminUseCase.InviteCommand(request.email(), request.role()));
        return InvitationResponse.from(result);
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public MembershipResponse acceptInvitation(@CurrentActor ActorContext actor,
            @PathVariable("invitationId") String invitationId) {
        MembershipResult result = membershipAdminService.acceptInvitation(actor, invitationId);
        return MembershipResponse.from(result);
    }

    @GetMapping("/memberships")
    @PreAuthorize("hasAnyAuthority('SCOPE_TENANT_READ_ONLY')")
    public List<MembershipResponse> listMemberships(@CurrentActor ActorContext actor) {
        return membershipAdminService.listMemberships(actor)
                .stream()
                .map(MembershipResponse::from)
                .toList();
    }

    @GetMapping("/memberships/{id}")
    @PreAuthorize("hasAnyAuthority('SCOPE_TENANT_READ_ONLY')")
    public MembershipDetailResponse getMembershipDetail(
            @CurrentActor ActorContext actor,
            @PathVariable("id") String membershipId) {
        MembershipDetailResult result = membershipAdminService.getMembershipDetail(actor, membershipId);
        return MembershipDetailResponse.from(result);
    }


    @PatchMapping("/memberships/{id}/status")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipResponse updateMembershipStatus(
            @CurrentActor ActorContext actor,
            @PathVariable("id") String membershipId,
            @RequestBody UpdateMembershipStatusRequest request) {
        MembershipResult result = membershipAdminService.updateMembershipStatus(
                actor,
                membershipId,
                new MembershipAdminUseCase.UpdateMembershipStatusCommand(request.status()));
        return MembershipResponse.from(result);
    }

    @GetMapping("/memberships/{id}/roles")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipRoleAssignmentsResponse listMembershipRoles(
            @CurrentActor ActorContext actor,
            @PathVariable("id") String membershipId) {
        MembershipRoleAssignmentsResult result = membershipAdminService.listMembershipRoleAssignments(actor,
                membershipId);
        return MembershipRoleAssignmentsResponse.from(result);
    }

    @GetMapping("/memberships/{id}/roles/available")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipAssignableRolesResponse listAssignableRoleCodes(
            @CurrentActor ActorContext actor,
            @PathVariable("id") String membershipId) {
        MembershipAssignableRolesResult result = membershipAdminService.listAssignableRoleCodes(actor, membershipId);
        return MembershipAssignableRolesResponse.from(result);
    }

    @GetMapping("/permission-templates/available")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public TenantAvailableTemplateCodesResponse listTenantAvailableTemplateCodes(
            @CurrentActor ActorContext actor) {
        TenantAvailableTemplateCodesResult result = membershipAdminService.listTenantAvailableTemplateCodes(actor);
        return TenantAvailableTemplateCodesResponse.from(result);
    }

    @PostMapping("/memberships/{id}/roles/{roleCode}")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipRoleAssignmentsResponse assignMembershipRole(
            @CurrentActor ActorContext actor,
            @PathVariable("id") String membershipId,
            @PathVariable("roleCode") String roleCode) {
        MembershipRoleAssignmentsResult result = membershipAdminService.assignMembershipRole(
                actor,
                membershipId,
                new MembershipAdminUseCase.AssignMembershipRoleCommand(roleCode));
        return MembershipRoleAssignmentsResponse.from(result);
    }

    @DeleteMapping("/memberships/{id}/roles/{roleCode}")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipRoleAssignmentsResponse revokeMembershipRole(
            @CurrentActor ActorContext actor,
            @PathVariable("id") String membershipId,
            @PathVariable("roleCode") String roleCode) {
        MembershipRoleAssignmentsResult result = membershipAdminService.revokeMembershipRole(
                actor,
                membershipId,
                new MembershipAdminUseCase.RevokeMembershipRoleCommand(roleCode));
        return MembershipRoleAssignmentsResponse.from(result);
    }

    @PostMapping("/memberships/{id}/permission-templates/{templateCode}/apply")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipPermissionTemplateResponse applyPermissionTemplate(
            @CurrentActor ActorContext actor,
            @PathVariable("id") String membershipId,
            @PathVariable("templateCode") String templateCode,
            @RequestBody(required = false) MutatePermissionTemplateRequest request) {
        MembershipPermissionTemplateResult result = membershipAdminService.applyPermissionTemplate(
                actor,
                membershipId,
                new MembershipAdminUseCase.ApplyPermissionTemplateCommand(templateCode,
                        request == null ? null : request.reason()));
        return MembershipPermissionTemplateResponse.from(result);
    }

    @PostMapping("/memberships/{id}/permission-templates/{templateCode}/revoke")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public MembershipPermissionTemplateResponse revokePermissionTemplate(
            @CurrentActor ActorContext actor,
            @PathVariable("id") String membershipId,
            @PathVariable("templateCode") String templateCode,
            @RequestBody(required = false) MutatePermissionTemplateRequest request) {
        MembershipPermissionTemplateResult result = membershipAdminService.revokePermissionTemplate(
                actor,
                membershipId,
                new MembershipAdminUseCase.RevokePermissionTemplateCommand(templateCode,
                        request == null ? null : request.reason()));
        return MembershipPermissionTemplateResponse.from(result);
    }

    // TODO: 초대 관리 API 추가
//  - 별도 API: GET /admin/invitations?status=PENDING
//  - 필요하면 PENDING/ACCEPTED/REVOKED 필터
//  - 응답에 invitationId, email, role, status, invitedAt, invitedBy, acceptLink(옵션) 포함
}
