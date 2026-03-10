package io.attestry.userauth.interfaces.membership;

import io.attestry.commonlib.web.CurrentActor;
import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.ApplyPermissionTemplateCommand;
import io.attestry.userauth.application.dto.command.AssignMembershipRoleCommand;
import io.attestry.userauth.application.dto.command.RevokeMembershipRoleCommand;
import io.attestry.userauth.application.dto.command.RevokePermissionTemplateCommand;
import io.attestry.userauth.application.dto.command.UpdateMembershipStatusCommand;
import io.attestry.userauth.application.dto.result.MembershipAssignableRolesResult;
import io.attestry.userauth.application.dto.result.MembershipDetailResult;
import io.attestry.userauth.application.dto.result.MembershipPermissionTemplateResult;
import io.attestry.userauth.application.dto.result.MembershipResult;
import io.attestry.userauth.application.dto.result.MembershipRoleAssignmentsResult;
import io.attestry.userauth.application.dto.result.TenantAvailableTemplateCodesResult;
import io.attestry.userauth.application.usecase.membership.MembershipCommandUseCase;
import io.attestry.userauth.application.usecase.membership.MembershipQueryUseCase;
import io.attestry.userauth.interfaces.membership.dto.request.MutatePermissionTemplateRequest;
import io.attestry.userauth.interfaces.membership.dto.request.UpdateMembershipStatusRequest;
import io.attestry.userauth.interfaces.membership.dto.response.MembershipAssignableRolesResponse;
import io.attestry.userauth.interfaces.membership.dto.response.MembershipDetailResponse;
import io.attestry.userauth.interfaces.membership.dto.response.MembershipPermissionTemplateResponse;
import io.attestry.userauth.interfaces.membership.dto.response.MembershipResponse;
import io.attestry.userauth.interfaces.membership.dto.response.MembershipRoleAssignmentsResponse;
import io.attestry.userauth.interfaces.membership.dto.response.TenantAvailableTemplateCodesResponse;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
public class MembershipManagementHttp {

    private final MembershipQueryUseCase membershipQueryUseCase;
    private final MembershipCommandUseCase membershipCommandUseCase;

    @GetMapping("/memberships")
    @PreAuthorize("hasAnyAuthority('SCOPE_TENANT_READ_ONLY')")
    public ApiResponse<List<MembershipResponse>> listMemberships(@CurrentActor ActorContext actor) {
        return ApiResponse.success(membershipQueryUseCase.listMemberships(actor)
                .stream()
                .map(MembershipResponse::from)
                .toList());
    }

    @GetMapping("/memberships/{id}")
    @PreAuthorize("hasAnyAuthority('SCOPE_TENANT_READ_ONLY')")
    public ApiResponse<MembershipDetailResponse> getMembershipDetail(
            @CurrentActor ActorContext actor,
            @PathVariable("id") String membershipId) {
        MembershipDetailResult result = membershipQueryUseCase.getMembershipDetail(actor, membershipId);
        return ApiResponse.success(MembershipDetailResponse.from(result));
    }

    @PatchMapping("/memberships/{id}/status")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public ApiResponse<MembershipResponse> updateMembershipStatus(
            @CurrentActor ActorContext actor,
            @PathVariable("id") String membershipId,
            @Valid @RequestBody UpdateMembershipStatusRequest request) {
        MembershipResult result = membershipCommandUseCase.updateMembershipStatus(
                actor,
                membershipId,
                new UpdateMembershipStatusCommand(request.status()));
        return ApiResponse.success(MembershipResponse.from(result));
    }

    @GetMapping("/memberships/{id}/roles")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public ApiResponse<MembershipRoleAssignmentsResponse> listMembershipRoles(
            @CurrentActor ActorContext actor,
            @PathVariable("id") String membershipId) {
        MembershipRoleAssignmentsResult result = membershipQueryUseCase.listMembershipRoleAssignments(actor, membershipId);
        return ApiResponse.success(MembershipRoleAssignmentsResponse.from(result));
    }

    @GetMapping("/memberships/{id}/roles/available")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public ApiResponse<MembershipAssignableRolesResponse> listAssignableRoleCodes(
            @CurrentActor ActorContext actor,
            @PathVariable("id") String membershipId) {
        MembershipAssignableRolesResult result = membershipQueryUseCase.listAssignableRoleCodes(actor, membershipId);
        return ApiResponse.success(MembershipAssignableRolesResponse.from(result));
    }

    @GetMapping("/permission-templates/available")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public ApiResponse<TenantAvailableTemplateCodesResponse> listTenantAvailableTemplateCodes(
            @CurrentActor ActorContext actor) {
        TenantAvailableTemplateCodesResult result = membershipQueryUseCase.listTenantAvailableTemplateCodes(actor);
        return ApiResponse.success(TenantAvailableTemplateCodesResponse.from(result));
    }

    @PostMapping("/memberships/{id}/roles/{roleCode}")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public ApiResponse<MembershipRoleAssignmentsResponse> assignMembershipRole(
            @CurrentActor ActorContext actor,
            @PathVariable("id") String membershipId,
            @PathVariable("roleCode") String roleCode) {
        MembershipRoleAssignmentsResult result = membershipCommandUseCase.assignMembershipRole(
                actor,
                membershipId,
                new AssignMembershipRoleCommand(roleCode));
        return ApiResponse.success(MembershipRoleAssignmentsResponse.from(result));
    }

    @DeleteMapping("/memberships/{id}/roles/{roleCode}")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public ApiResponse<MembershipRoleAssignmentsResponse> revokeMembershipRole(
            @CurrentActor ActorContext actor,
            @PathVariable("id") String membershipId,
            @PathVariable("roleCode") String roleCode) {
        MembershipRoleAssignmentsResult result = membershipCommandUseCase.revokeMembershipRole(
                actor,
                membershipId,
                new RevokeMembershipRoleCommand(roleCode));
        return ApiResponse.success(MembershipRoleAssignmentsResponse.from(result));
    }

    @PostMapping("/memberships/{id}/permission-templates/{templateCode}/apply")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public ApiResponse<MembershipPermissionTemplateResponse> applyPermissionTemplate(
            @CurrentActor ActorContext actor,
            @PathVariable("id") String membershipId,
            @PathVariable("templateCode") String templateCode,
            @RequestBody(required = false) MutatePermissionTemplateRequest request) {
        MembershipPermissionTemplateResult result = membershipCommandUseCase.applyPermissionTemplate(
                actor,
                membershipId,
                new ApplyPermissionTemplateCommand(
                        templateCode,
                        request == null ? null : request.reason()));
        return ApiResponse.success(MembershipPermissionTemplateResponse.from(result));
    }

    @PostMapping("/memberships/{id}/permission-templates/{templateCode}/revoke")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
    public ApiResponse<MembershipPermissionTemplateResponse> revokePermissionTemplate(
            @CurrentActor ActorContext actor,
            @PathVariable("id") String membershipId,
            @PathVariable("templateCode") String templateCode,
            @RequestBody(required = false) MutatePermissionTemplateRequest request) {
        MembershipPermissionTemplateResult result = membershipCommandUseCase.revokePermissionTemplate(
                actor,
                membershipId,
                new RevokePermissionTemplateCommand(
                        templateCode,
                        request == null ? null : request.reason()));
        return ApiResponse.success(MembershipPermissionTemplateResponse.from(result));
    }
}
