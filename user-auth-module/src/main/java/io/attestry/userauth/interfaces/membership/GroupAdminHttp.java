package io.attestry.userauth.interfaces.membership;

import io.attestry.userauth.application.dto.result.GroupAdminResult;
import io.attestry.userauth.application.usecase.membership.MembershipAdminUseCase;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.interfaces.membership.dto.response.GroupResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class GroupAdminHttp {

    private final MembershipAdminUseCase membershipAdminService;

    public GroupAdminHttp(MembershipAdminUseCase membershipAdminService) {
        this.membershipAdminService = membershipAdminService;
    }

    @PostMapping("/tenants/{tenantId}/admin/groups/{id}/suspend")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_GROUP_SUSPEND')")
    public GroupResponse suspendGroup(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String groupId
    ) {
        GroupAdminResult result = membershipAdminService.suspendGroup(principal, tenantId, groupId);
        return GroupResponse.from(result);
    }

    @PostMapping("/tenants/{tenantId}/admin/groups/{id}/unsuspend")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_GROUP_RESUME')")
    public GroupResponse unsuspendGroup(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String groupId
    ) {
        GroupAdminResult result = membershipAdminService.unsuspendGroup(principal, tenantId, groupId);
        return GroupResponse.from(result);
    }

}
