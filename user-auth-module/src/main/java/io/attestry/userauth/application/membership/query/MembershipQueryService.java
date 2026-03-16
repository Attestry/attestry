package io.attestry.userauth.application.membership.query;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.result.MembershipAssignableRolesResult;
import io.attestry.userauth.application.dto.result.MembershipDetailResult;
import io.attestry.userauth.application.dto.result.MembershipRoleAssignmentsResult;
import io.attestry.userauth.application.dto.result.TenantAvailableTemplateCodesResult;
import io.attestry.userauth.application.dto.view.MembershipAdminView;
import io.attestry.userauth.application.dto.view.MembershipView;
import io.attestry.userauth.application.membership.assembler.MembershipQueryViewAssembler;
import io.attestry.userauth.application.membership.policy.MembershipAccessPolicy;
import io.attestry.userauth.application.membership.policy.MembershipQueryAccessPolicy;
import io.attestry.userauth.application.port.membership.MembershipPort;
import io.attestry.userauth.application.port.membership.MembershipProjectionPort;
import io.attestry.userauth.application.port.template.TenantRoleTemplateBindingPort;
import io.attestry.userauth.application.port.identity.UserAccountRepositoryPort;
import io.attestry.userauth.application.usecase.membership.MembershipQueryUseCase;
import java.util.List;
import java.util.Set;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.identity.model.UserAccount;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.service.RoleAssignmentDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MembershipQueryService implements MembershipQueryUseCase {

    private final MembershipPort membershipPort;
    private final MembershipProjectionPort membershipProjectionPort;
    private final UserAccountRepositoryPort userAccountRepository;
    private final TenantRoleTemplateBindingPort tenantRoleTemplateBindingPort;
    private final MembershipAccessPolicy membershipAccessPolicy;
    private final MembershipQueryAccessPolicy queryAccessPolicy;
    private final MembershipQueryViewAssembler viewAssembler;
    private final RoleAssignmentDomainService roleAssignmentDomainService;

    @Override
    public List<MembershipView> getMemberships(String userId) {
        return membershipPort.findByUserId(userId).stream()
            .map(viewAssembler::toMembershipView)
            .toList();
    }
    @Override
    @Transactional(readOnly = true)
    public List<MembershipAdminView> listMemberships(ActorContext actor) {
        String tenantId = actor.tenantId();
        return membershipPort.findMembershipsByTenantId(tenantId).stream()
                .map(viewAssembler::toAdminView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipDetailResult getMembershipDetail(ActorContext actor, String membershipId) {
        Membership membership = membershipAccessPolicy.loadTenantMembership(membershipId, actor.tenantId());

        UserAccount userAccount = userAccountRepository.findById(membership.userId())
                .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.USER_NOT_FOUND, "사용자 정보를 찾을 수 없습니다"));

        return viewAssembler.toDetailResult(membership, userAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipRoleAssignmentsResult listMembershipRoleAssignments(ActorContext actor, String membershipId) {
        Membership target = membershipAccessPolicy.loadTenantMembership(membershipId, actor.tenantId());
        return new MembershipRoleAssignmentsResult(membershipId, target.currentRoleCodes().stream().sorted().toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipAssignableRolesResult listAssignableRoleCodes(ActorContext actor, String membershipId) {
        String tenantId = actor.tenantId();
        membershipAccessPolicy.loadTenantMembership(membershipId, tenantId);

        Membership actorMembership = membershipAccessPolicy.resolveActiveActorMembership(actor.userId(), tenantId);
        Set<String> actorRoles = actorMembership.currentRoleCodes();
        List<String> assignableRoleCodes = membershipProjectionPort.findGlobalEnabledRoleCodes().stream()
                .map(roleCode -> roleAssignmentDomainService.evaluate(
                        actorRoles,
                        actorMembership.membershipId(),
                        membershipId,
                        roleCode))
                .filter(evaluation -> !evaluation.denied())
                .filter(evaluation -> queryAccessPolicy.isRoleAssignableByAuthorization(actor, tenantId, membershipId, evaluation))
                .map(RoleAssignmentDomainService.Evaluation::normalizedRoleCode)
                .sorted()
                .toList();

        return new MembershipAssignableRolesResult(membershipId, assignableRoleCodes);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantAvailableTemplateCodesResult listTenantAvailableTemplateCodes(ActorContext actor) {
        String tenantId = actor.tenantId();
        membershipAccessPolicy.resolveActiveActorMembership(actor.userId(), tenantId);
        List<String> templateCodes = tenantRoleTemplateBindingPort.findTenantRoleTemplateBindings(tenantId).stream()
                .filter(TenantRoleTemplateBindingPort.TenantRoleTemplateBindingView::enabled)
                .map(TenantRoleTemplateBindingPort.TenantRoleTemplateBindingView::templateCode)
                .distinct()
                .sorted()
                .toList();
        return new TenantAvailableTemplateCodesResult(tenantId, templateCodes);
    }

}
