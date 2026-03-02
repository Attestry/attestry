package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.application.port.MembershipAdminRepositoryPort;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.membership.model.Invitation;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.membership.policy.DefaultMembershipRolePolicy;
import io.attestry.userauth.domain.organization.model.GroupStatus;
import io.attestry.userauth.domain.organization.model.TenantStatus;
import io.attestry.userauth.domain.user.vo.Email;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.InvitationJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipRoleAssignmentJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.GroupJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.InvitationJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.MembershipJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.MembershipRoleAssignmentJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.RoleJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.UserAccountJpaRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JpaMembershipAdminRepositoryAdapter implements MembershipAdminRepositoryPort {

    private final InvitationJpaRepository invitationRepository;
    private final MembershipJpaRepository membershipRepository;
    private final MembershipRoleAssignmentJpaRepository membershipRoleAssignmentRepository;
    private final RoleJpaRepository roleRepository;
    private final UserAccountJpaRepository userAccountRepository;
    private final GroupJpaRepository groupRepository;
    private final JdbcTemplate jdbcTemplate;

    public JpaMembershipAdminRepositoryAdapter(
        InvitationJpaRepository invitationRepository,
        MembershipJpaRepository membershipRepository,
        MembershipRoleAssignmentJpaRepository membershipRoleAssignmentRepository,
        RoleJpaRepository roleRepository,
        UserAccountJpaRepository userAccountRepository,
        GroupJpaRepository groupRepository,
        JdbcTemplate jdbcTemplate
    ) {
        this.invitationRepository = invitationRepository;
        this.membershipRepository = membershipRepository;
        this.membershipRoleAssignmentRepository = membershipRoleAssignmentRepository;
        this.roleRepository = roleRepository;
        this.userAccountRepository = userAccountRepository;
        this.groupRepository = groupRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UserProfileView> findUserById(String userId) {
        return userAccountRepository.findById(userId)
            .map(user -> new UserProfileView(user.getUserId(), user.getEmail()));
    }

    @Override
    public Optional<GroupView> findGroupById(String groupId) {
        return groupRepository.findById(groupId)
            .map(group -> new GroupView(group.getGroupId(), group.getTenantId(), group.getType(), group.getStatus()));
    }

    @Override
    public GroupView saveGroup(GroupView group) {
        io.attestry.userauth.infrastructure.persistence.jpa.entity.GroupJpaEntity saved = groupRepository.save(
            new io.attestry.userauth.infrastructure.persistence.jpa.entity.GroupJpaEntity(
                group.groupId(),
                group.tenantId(),
                group.type(),
                group.status()
            )
        );
        return new GroupView(saved.getGroupId(), saved.getTenantId(), saved.getType(), saved.getStatus());
    }

    @Override
    public void updateGroupStatusOnMemberships(String groupId, GroupStatus status) {
        List<MembershipJpaEntity> memberships = membershipRepository.findByGroupId(groupId);
        if (memberships.isEmpty()) {
            return;
        }
        List<MembershipJpaEntity> updated = memberships.stream()
            .map(current -> new MembershipJpaEntity(
                current.getMembershipId(),
                current.getUserId(),
                current.getGroupId(),
                current.getTenantId(),
                current.getGroupType(),
                current.getRole(),
                current.getStatus(),
                status,
                current.getTenantStatus()
            ))
            .toList();
        membershipRepository.saveAll(updated);
    }

    @Override
    public Invitation saveInvitation(Invitation invitation) {
        InvitationJpaEntity saved = invitationRepository.save(new InvitationJpaEntity(
            invitation.invitationId(),
            invitation.tenantId(),
            invitation.groupId(),
            invitation.inviteeEmail().value(),
            invitation.role(),
            invitation.status(),
            invitation.invitedBy(),
            invitation.invitedAt(),
            invitation.acceptedBy(),
            invitation.acceptedAt()
        ));
        return toDomain(saved);
    }

    @Override
    public Optional<Invitation> findInvitationById(String invitationId) {
        return invitationRepository.findById(invitationId).map(this::toDomain);
    }

    @Override
    public Membership createMembership(String userId, String groupId, String tenantId, MembershipRole role) {
        GroupView group = findGroupById(groupId)
            .orElseThrow(() -> new DomainException(ErrorCode.GROUP_NOT_FOUND, "Group not found"));
        if (!tenantId.equals(group.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership creation denied");
        }
        MembershipJpaEntity saved = membershipRepository.save(new MembershipJpaEntity(
            UUID.randomUUID().toString(),
            userId,
            groupId,
            tenantId,
            group.type(),
            role,
            MembershipStatus.ACTIVE,
            group.status(),
            TenantStatus.ACTIVE
        ));
        membershipRoleAssignmentRepository.save(new MembershipRoleAssignmentJpaEntity(
            UUID.randomUUID().toString(),
            saved.getMembershipId(),
            resolveGlobalRoleIdByCode(DefaultMembershipRolePolicy.resolveGlobalRoleCode(saved.getRole(), saved.getGroupType())),
            null,
            Instant.now()
        ));
        return toDomain(saved);
    }

    @Override
    public List<Membership> findMembershipsByTenantId(String tenantId) {
        return membershipRepository.findByTenantId(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Membership> findMembershipById(String membershipId) {
        return membershipRepository.findById(membershipId).map(this::toDomain);
    }

    @Override
    public Membership updateMembership(String tenantId, String membershipId, MembershipRole role, MembershipStatus status) {
        MembershipJpaEntity current = membershipRepository.findByMembershipIdAndTenantId(membershipId, tenantId)
            .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
        MembershipJpaEntity saved = membershipRepository.save(new MembershipJpaEntity(
            current.getMembershipId(),
            current.getUserId(),
            current.getGroupId(),
            current.getTenantId(),
            current.getGroupType(),
            role,
            status,
            current.getGroupStatus(),
            current.getTenantStatus()
        ));
        String expectedRoleId = resolveGlobalRoleIdByCode(DefaultMembershipRolePolicy.resolveGlobalRoleCode(saved.getRole(), saved.getGroupType()));
        MembershipRoleAssignmentJpaEntity assignment = membershipRoleAssignmentRepository.findByMembershipId(saved.getMembershipId()).orElse(null);
        if (assignment == null || !expectedRoleId.equals(assignment.getRoleId())) {
            membershipRoleAssignmentRepository.save(new MembershipRoleAssignmentJpaEntity(
                assignment == null ? UUID.randomUUID().toString() : assignment.getAssignmentId(),
                saved.getMembershipId(),
                expectedRoleId,
                null,
                Instant.now()
            ));
        }
        return toDomain(saved);
    }

    @Override
    public Set<String> findRoleCodesByMembershipId(String membershipId) {
        return Set.copyOf(membershipRoleAssignmentRepository.findRoleCodesByMembershipId(membershipId));
    }

    @Override
    public Set<String> findGlobalEnabledRoleCodes() {
        return roleRepository.findByTenantIdIsNullAndEnabledTrue().stream()
            .map(role -> role.getCode().trim().toUpperCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    @Override
    public void upsertPermissionOverrides(
        String membershipId,
        Set<String> permissionCodes,
        String source,
        String reason,
        String actorUserId,
        Instant now
    ) {
        for (String permissionCode : permissionCodes) {
            int updated = jdbcTemplate.update(
                """
                    INSERT INTO membership_permission_overrides (
                        override_id,
                        membership_id,
                        permission_id,
                        effect,
                        source,
                        reason,
                        created_by_user_id,
                        created_at
                    )
                    SELECT ?, ?, p.permission_id, 'ALLOW', ?, ?, ?, ?
                    FROM permissions p
                    WHERE p.code = ?
                      AND p.enabled = TRUE
                    ON CONFLICT (membership_id, permission_id)
                    DO UPDATE SET
                        effect = EXCLUDED.effect,
                        source = EXCLUDED.source,
                        reason = EXCLUDED.reason,
                        created_by_user_id = EXCLUDED.created_by_user_id,
                        created_at = EXCLUDED.created_at
                    """,
                UUID.randomUUID().toString(),
                membershipId,
                source,
                reason,
                actorUserId,
                Timestamp.from(now),
                permissionCode
            );
            if (updated == 0) {
                throw new DomainException(ErrorCode.ROLE_NOT_FOUND, "Permission not found: " + permissionCode);
            }
        }
    }

    @Override
    public void deletePermissionOverrides(String membershipId, Set<String> permissionCodes) {
        for (String permissionCode : permissionCodes) {
            jdbcTemplate.update(
                """
                    DELETE FROM membership_permission_overrides
                    WHERE membership_id = ?
                      AND permission_id IN (
                          SELECT permission_id
                          FROM permissions
                          WHERE code = ?
                      )
                    """,
                membershipId,
                permissionCode
            );
        }
    }

    @Override
    public Set<String> applyPermissionTemplateToMembership(
        String membershipId,
        String templateCode,
        String reason,
        String actorUserId,
        Instant now
    ) {
        Set<String> permissionCodes = findEnabledPermissionCodesByTemplateCode(templateCode);
        for (String permissionCode : permissionCodes) {
            int updated = jdbcTemplate.update(
                """
                    INSERT INTO membership_permission_overrides (
                        override_id,
                        membership_id,
                        permission_id,
                        effect,
                        source,
                        reason,
                        created_by_user_id,
                        created_at
                    )
                    SELECT ?, ?, p.permission_id, 'ALLOW', 'TEMPLATE', ?, ?, ?
                    FROM permissions p
                    WHERE p.code = ?
                      AND p.enabled = TRUE
                    ON CONFLICT (membership_id, permission_id)
                    DO UPDATE SET
                        effect = EXCLUDED.effect,
                        source = EXCLUDED.source,
                        reason = EXCLUDED.reason,
                        created_by_user_id = EXCLUDED.created_by_user_id,
                        created_at = EXCLUDED.created_at
                    """,
                UUID.randomUUID().toString(),
                membershipId,
                reason,
                actorUserId,
                Timestamp.from(now),
                permissionCode
            );
            if (updated == 0) {
                throw new DomainException(ErrorCode.PERMISSION_NOT_FOUND, "Permission not found: " + permissionCode);
            }
        }
        return permissionCodes;
    }

    @Override
    public Set<String> revokePermissionTemplateFromMembership(String membershipId, String templateCode) {
        Set<String> permissionCodes = findEnabledPermissionCodesByTemplateCode(templateCode);
        deletePermissionOverrides(membershipId, permissionCodes);
        return permissionCodes;
    }

    @Override
    public Set<String> assignRoleToMembership(String membershipId, String roleCode, String actorUserId, Instant assignedAt) {
        String roleId = roleRepository.findByTenantIdIsNullAndCodeAndEnabledTrue(roleCode)
            .orElseThrow(() -> new DomainException(ErrorCode.ROLE_NOT_FOUND, "Role not found"))
            .getRoleId();
        MembershipRoleAssignmentJpaEntity current = membershipRoleAssignmentRepository.findByMembershipId(membershipId).orElse(null);
        if (current != null && roleId.equals(current.getRoleId())) {
            return findRoleCodesByMembershipId(membershipId);
        }
        membershipRoleAssignmentRepository.save(new MembershipRoleAssignmentJpaEntity(
            current == null ? UUID.randomUUID().toString() : current.getAssignmentId(),
            membershipId,
            roleId,
            actorUserId,
            assignedAt
        ));
        return findRoleCodesByMembershipId(membershipId);
    }

    @Override
    public Set<String> revokeRoleFromMembership(String membershipId, String roleCode) {
        String roleId = roleRepository.findByTenantIdIsNullAndCodeAndEnabledTrue(roleCode)
            .orElseThrow(() -> new DomainException(ErrorCode.ROLE_NOT_FOUND, "Role not found"))
            .getRoleId();
        MembershipRoleAssignmentJpaEntity assignment = membershipRoleAssignmentRepository.findByMembershipIdAndRoleId(membershipId, roleId)
            .orElseThrow(() -> new DomainException(ErrorCode.ROLE_ASSIGNMENT_NOT_FOUND, "Role assignment not found"));
        membershipRoleAssignmentRepository.deleteByMembershipIdAndRoleId(assignment.getMembershipId(), assignment.getRoleId());
        return findRoleCodesByMembershipId(membershipId);
    }

    private String resolveGlobalRoleIdByCode(String roleCode) {
        return roleRepository.findByTenantIdIsNullAndCodeAndEnabledTrue(roleCode)
            .orElseThrow(() -> new DomainException(ErrorCode.ROLE_NOT_FOUND, "Role not found"))
            .getRoleId();
    }

    private Set<String> findEnabledPermissionCodesByTemplateCode(String templateCode) {
        List<String> codes = jdbcTemplate.queryForList(
            """
                SELECT p.code
                FROM permission_templates pt
                JOIN template_permissions tp ON tp.template_id = pt.template_id
                JOIN permissions p ON p.permission_id = tp.permission_id
                WHERE pt.code = ?
                  AND pt.enabled = TRUE
                  AND p.enabled = TRUE
                ORDER BY p.code
                """,
            String.class,
            templateCode
        );
        if (codes.isEmpty()) {
            boolean exists = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                """
                    SELECT EXISTS(
                        SELECT 1
                        FROM permission_templates
                        WHERE code = ?
                    )
                    """,
                Boolean.class,
                templateCode
            ));
            if (!exists) {
                throw new DomainException(ErrorCode.TEMPLATE_NOT_FOUND, "Permission template not found");
            }
        }
        return Set.copyOf(codes);
    }

    private Invitation toDomain(InvitationJpaEntity entity) {
        return new Invitation(
            entity.getInvitationId(),
            entity.getTenantId(),
            entity.getGroupId(),
            Email.of(entity.getInviteeEmail()),
            entity.getRole(),
            entity.getStatus(),
            entity.getInvitedBy(),
            entity.getInvitedAt(),
            entity.getAcceptedBy(),
            entity.getAcceptedAt()
        );
    }

    private Membership toDomain(MembershipJpaEntity entity) {
        return new Membership(
            entity.getMembershipId(),
            entity.getUserId(),
            entity.getGroupId(),
            entity.getTenantId(),
            entity.getGroupType(),
            entity.getRole(),
            entity.getStatus(),
            entity.getGroupStatus(),
            entity.getTenantStatus()
        );
    }
}
