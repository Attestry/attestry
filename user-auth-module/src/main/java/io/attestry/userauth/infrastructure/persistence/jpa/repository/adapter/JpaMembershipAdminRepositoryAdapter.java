package io.attestry.userauth.infrastructure.persistence.jpa.repository.adapter;

import io.attestry.userauth.application.port.MembershipPort;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.membership.model.RoleAssignment;
import io.attestry.userauth.application.port.TenantRepositoryPort;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipRoleAssignmentJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.MembershipJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.MembershipRoleAssignmentJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.RoleJpaRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class JpaMembershipAdminRepositoryAdapter implements MembershipPort {

    private final MembershipJpaRepository membershipRepository;
    private final MembershipRoleAssignmentJpaRepository membershipRoleAssignmentRepository;
    private final RoleJpaRepository roleRepository;
    private final TenantRepositoryPort tenantRepository;
    private final JdbcTemplate jdbcTemplate;
    private final MembershipEffectivePermissionProjectionRefresher permissionProjectionRefresher;


    @Override
    public List<Membership> findMembershipsByTenantId(String tenantId) {
        return membershipRepository.findByTenantId(tenantId).stream().map(this::toMembershipDomainWithRoles).toList();
    }

    @Override
    public Optional<Membership> findMembershipById(String membershipId) {
        return membershipRepository.findById(membershipId).map(this::toMembershipDomainWithRoles);
    }

    @Override
    public Membership updateMembership(String tenantId, String membershipId, MembershipRole role, MembershipStatus status) {
        MembershipJpaEntity current = membershipRepository.findByMembershipIdAndTenantId(membershipId, tenantId)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
        MembershipJpaEntity saved = membershipRepository.save(new MembershipJpaEntity(
            current.getMembershipId(),
            current.getUserId(),
            current.getTenantId(),
            current.getTenantType(),
            role,
            status,
            current.getTenantStatus()
        ));
        permissionProjectionRefresher.refreshMembership(saved.getMembershipId());
        return toMembershipDomainWithRoles(saved);
    }

    public void upsertPermissionOverrides(
        String membershipId,
        Set<String> permissionCodes,
        String source,
        String reason,
        String actorUserId,
        Instant now
    ) {
        for (String permissionCode : permissionCodes) {
            String permissionId = jdbcTemplate.queryForObject(
                """
                    SELECT permission_id
                    FROM permissions
                    WHERE code = ?
                      AND enabled = TRUE
                    """,
                String.class,
                permissionCode
            );
            if (permissionId == null) {
                throw new UserAuthDomainException(UserAuthErrorCode.ROLE_NOT_FOUND, "Permission not found: " + permissionCode);
            }
            int updated = jdbcTemplate.update(
                """
                    UPDATE membership_permission_overrides
                    SET effect = 'ALLOW',
                        source = ?,
                        reason = ?,
                        created_by_user_id = ?,
                        created_at = ?
                    WHERE membership_id = ?
                      AND permission_id = ?
                    """,
                source,
                reason,
                actorUserId,
                Timestamp.from(now),
                membershipId,
                permissionId
            );
            if (updated == 0) {
                jdbcTemplate.update(
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
                        VALUES (?, ?, ?, 'ALLOW', ?, ?, ?, ?)
                        """,
                    UUID.randomUUID().toString(),
                    membershipId,
                    permissionId,
                    source,
                    reason,
                    actorUserId,
                    Timestamp.from(now)
                );
            }
        }
        permissionProjectionRefresher.refreshMembership(membershipId);
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
        permissionProjectionRefresher.refreshMembership(membershipId);
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
            String permissionId = jdbcTemplate.queryForObject(
                """
                    SELECT permission_id
                    FROM permissions
                    WHERE code = ?
                      AND enabled = TRUE
                    """,
                String.class,
                permissionCode
            );
            if (permissionId == null) {
                throw new UserAuthDomainException(UserAuthErrorCode.PERMISSION_NOT_FOUND, "Permission not found: " + permissionCode);
            }
            int updated = jdbcTemplate.update(
                """
                    UPDATE membership_permission_overrides
                    SET effect = 'ALLOW',
                        source = 'TEMPLATE',
                        reason = ?,
                        created_by_user_id = ?,
                        created_at = ?
                    WHERE membership_id = ?
                      AND permission_id = ?
                    """,
                reason,
                actorUserId,
                Timestamp.from(now),
                membershipId,
                permissionId
            );
            if (updated == 0) {
                jdbcTemplate.update(
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
                        VALUES (?, ?, ?, 'ALLOW', 'TEMPLATE', ?, ?, ?)
                        """,
                    UUID.randomUUID().toString(),
                    membershipId,
                    permissionId,
                    reason,
                    actorUserId,
                    Timestamp.from(now)
                );
            }
        }
        permissionProjectionRefresher.refreshMembership(membershipId);
        return permissionCodes;
    }

    @Override
    public Set<String> revokePermissionTemplateFromMembership(String membershipId, String templateCode) {
        Set<String> permissionCodes = findEnabledPermissionCodesByTemplateCode(templateCode);
        deletePermissionOverrides(membershipId, permissionCodes);
        return permissionCodes;
    }

    private String resolveGlobalRoleIdByCode(String roleCode) {
        return roleRepository.findByTenantIdIsNullAndCodeAndEnabledTrue(roleCode)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.ROLE_NOT_FOUND, "Role not found"))
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
                throw new UserAuthDomainException(UserAuthErrorCode.TEMPLATE_NOT_FOUND, "Permission template not found");
            }
        }
        return Set.copyOf(codes);
    }

    @Override
    public Optional<Membership> findById(String membershipId) {
        return findMembershipById(membershipId);
    }

    @Override
    public Membership save(Membership membership) {
        MembershipJpaEntity saved = membershipRepository.save(new MembershipJpaEntity(
            membership.membershipId(),
            membership.userId(),
            membership.tenantId(),
            membership.groupType(),
            membership.role(),
            membership.status(),
            membership.tenantStatus()
        ));
        syncRoleAssignments(membership);
        permissionProjectionRefresher.refreshMembership(saved.getMembershipId());
        return membership;
    }

    @Override
    public List<Membership> findByUserId(String userId) {
        return membershipRepository.findByUserId(userId).stream().map(this::toMembershipDomainWithRoles).toList();
    }

    @Override
    public Optional<Membership> findByUserIdAndTenantId(String userId, String tenantId) {
        return membershipRepository.findByUserIdAndTenantId(userId, tenantId).map(this::toMembershipDomainWithRoles);
    }

    @Override
    public List<Membership> findByTenantId(String tenantId) {
        return findMembershipsByTenantId(tenantId);
    }

    @Override
    public void assignRole(String membershipId, String roleCode, String assignedByUserId) {
        String roleId = resolveGlobalRoleIdByCode(roleCode);
        MembershipRoleAssignmentJpaEntity current =
            membershipRoleAssignmentRepository.findByMembershipId(membershipId).orElse(null);
        membershipRoleAssignmentRepository.save(new MembershipRoleAssignmentJpaEntity(
            current == null ? UUID.randomUUID().toString() : current.getAssignmentId(),
            membershipId,
            roleId,
            assignedByUserId,
            Instant.now()
        ));
    }

    private void syncRoleAssignments(Membership membership) {
        Set<RoleAssignment> desired = membership.roleAssignments();
        List<MembershipRoleAssignmentJpaEntity> existing = membershipRoleAssignmentRepository
            .findAllByMembershipId(membership.membershipId());

        Set<String> existingCodes = new HashSet<>();
        for (MembershipRoleAssignmentJpaEntity e : existing) {
            List<String> codes = membershipRoleAssignmentRepository.findRoleCodesByMembershipId(membership.membershipId());
            existingCodes.addAll(codes);
            break;
        }

        Set<String> desiredCodes = desired.stream().map(RoleAssignment::roleCode).collect(Collectors.toSet());

        // Remove assignments not in desired set
        for (MembershipRoleAssignmentJpaEntity e : existing) {
            String code = findRoleCodeByRoleId(e.getRoleId());
            if (code != null && !desiredCodes.contains(code)) {
                membershipRoleAssignmentRepository.deleteByMembershipIdAndRoleId(e.getMembershipId(), e.getRoleId());
            }
        }

        // Add/update assignments in desired set
        for (RoleAssignment ra : desired) {
            String roleId = roleRepository.findByTenantIdIsNullAndCodeAndEnabledTrue(ra.roleCode())
                .map(r -> r.getRoleId())
                .orElse(null);
            if (roleId == null) continue;

            Optional<MembershipRoleAssignmentJpaEntity> existingAssignment =
                membershipRoleAssignmentRepository.findByMembershipIdAndRoleId(membership.membershipId(), roleId);
            if (existingAssignment.isEmpty()) {
                membershipRoleAssignmentRepository.save(new MembershipRoleAssignmentJpaEntity(
                    UUID.randomUUID().toString(),
                    membership.membershipId(),
                    roleId,
                    ra.assignedByUserId(),
                    ra.assignedAt()
                ));
            }
        }
    }

    private String findRoleCodeByRoleId(String roleId) {
        return roleRepository.findById(roleId)
            .map(r -> r.getCode().trim().toUpperCase(Locale.ROOT))
            .orElse(null);
    }

    private Set<RoleAssignment> loadRoleAssignments(String membershipId) {
        List<MembershipRoleAssignmentJpaEntity> entities = membershipRoleAssignmentRepository.findAllByMembershipId(membershipId);
        Set<RoleAssignment> assignments = new HashSet<>();
        for (MembershipRoleAssignmentJpaEntity e : entities) {
            String code = findRoleCodeByRoleId(e.getRoleId());
            if (code != null) {
                assignments.add(new RoleAssignment(code, null, null));
            }
        }
        return assignments;
    }

    private Membership toMembershipDomainWithRoles(MembershipJpaEntity entity) {
        Set<RoleAssignment> roles = loadRoleAssignments(entity.getMembershipId());
        return toMembershipDomain(entity, roles);
    }

    private Membership toMembershipDomain(MembershipJpaEntity entity, Set<RoleAssignment> roleAssignments) {
        return Membership.reconstitute(
            entity.getMembershipId(),
            entity.getUserId(),
            entity.getTenantId(),
            entity.getTenantType(),
            entity.getRole(),
            entity.getStatus(),
            entity.getTenantStatus(),
            roleAssignments
        );
    }
}
