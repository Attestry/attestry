package io.attestry.userauth.application.usecase.membership;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.result.MembershipAssignableRolesResult;
import io.attestry.userauth.application.dto.result.MembershipDetailResult;
import io.attestry.userauth.application.dto.result.MembershipRoleAssignmentsResult;
import io.attestry.userauth.application.dto.result.TenantAvailableTemplateCodesResult;
import io.attestry.userauth.application.dto.view.MembershipAdminView;
import io.attestry.userauth.application.dto.view.MembershipView;
import java.util.List;

public interface MembershipQueryUseCase {
    List<MembershipView> getMemberships(String userId);

    List<MembershipAdminView> listMemberships(ActorContext actor);

    MembershipDetailResult getMembershipDetail(ActorContext actor, String membershipId);


    MembershipRoleAssignmentsResult listMembershipRoleAssignments(ActorContext actor, String membershipId);

    MembershipAssignableRolesResult listAssignableRoleCodes(ActorContext actor, String membershipId);

    TenantAvailableTemplateCodesResult listTenantAvailableTemplateCodes(ActorContext actor);

}
