package io.attestry.userauth.application.membership.query;

import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.membership.view.MembershipAssignableRolesView;
import io.attestry.userauth.application.membership.view.MembershipDetailView;
import io.attestry.userauth.application.membership.view.MembershipRoleAssignmentsView;
import io.attestry.userauth.application.membership.view.MembershipAdminView;
import io.attestry.userauth.application.membership.view.MembershipView;
import io.attestry.userauth.application.membership.view.TenantAvailableTemplateCodesView;
import java.util.List;

public interface MembershipQueryUseCase {
    List<MembershipView> getMemberships(String userId);

    List<MembershipAdminView> listMemberships(ActorContext actor);

    MembershipDetailView getMembershipDetail(ActorContext actor, String membershipId);


    MembershipRoleAssignmentsView listMembershipRoleAssignments(ActorContext actor, String membershipId);

    MembershipAssignableRolesView listAssignableRoleCodes(ActorContext actor, String membershipId);

    TenantAvailableTemplateCodesView listTenantAvailableTemplateCodes(ActorContext actor);

}
