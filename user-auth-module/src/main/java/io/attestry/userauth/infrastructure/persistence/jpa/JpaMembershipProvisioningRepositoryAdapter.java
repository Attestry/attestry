package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.application.port.MembershipProvisioningRepositoryPort;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.RoleAssignment;
import io.attestry.userauth.domain.membership.policy.DefaultMembershipRolePolicy;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipRoleAssignmentJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.MembershipRoleAssignmentJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.MembershipJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.RoleJpaRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaMembershipProvisioningRepositoryAdapter implements MembershipProvisioningRepositoryPort {

    private final MembershipJpaRepository repository;
    private final MembershipRoleAssignmentJpaRepository membershipRoleAssignmentRepository;
    private final RoleJpaRepository roleRepository;

    public JpaMembershipProvisioningRepositoryAdapter(
        MembershipJpaRepository repository,
        MembershipRoleAssignmentJpaRepository membershipRoleAssignmentRepository,
        RoleJpaRepository roleRepository
    ) {
        this.repository = repository;
        this.membershipRoleAssignmentRepository = membershipRoleAssignmentRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public Membership save(Membership membership) {
        MembershipJpaEntity saved = repository.save(new MembershipJpaEntity(
            membership.membershipId(),
            membership.userId(),
            membership.groupId(),
            membership.tenantId(),
            membership.groupType(),
            membership.role(),
            membership.status(),
            membership.groupStatus(),
            membership.tenantStatus()
        ));

        if (membershipRoleAssignmentRepository.findByMembershipId(saved.getMembershipId()).isEmpty()) {
            String defaultRoleCode = DefaultMembershipRolePolicy.resolveGlobalRoleCode(saved.getRole(), saved.getGroupType());
            membershipRoleAssignmentRepository.save(new MembershipRoleAssignmentJpaEntity(
                UUID.randomUUID().toString(),
                saved.getMembershipId(),
                resolveGlobalRoleIdByCode(defaultRoleCode),
                null,
                Instant.now()
            ));
        }

        return membership;
    }

    @Override
    public void assignRole(String membershipId, String roleCode, String assignedByUserId) {
        String roleId = roleRepository.findByTenantIdIsNullAndCodeAndEnabledTrue(roleCode)
            .orElseThrow(() -> new DomainException(ErrorCode.ROLE_NOT_FOUND, "Role not found"))
            .getRoleId();
        MembershipRoleAssignmentJpaEntity current = membershipRoleAssignmentRepository.findByMembershipId(membershipId).orElse(null);
        membershipRoleAssignmentRepository.save(new MembershipRoleAssignmentJpaEntity(
            current == null ? UUID.randomUUID().toString() : current.getAssignmentId(),
            membershipId,
            roleId,
            assignedByUserId,
            Instant.now()
        ));
    }

    private String resolveGlobalRoleIdByCode(String roleCode) {
        return roleRepository.findByTenantIdIsNullAndCodeAndEnabledTrue(roleCode)
            .orElseThrow(() -> new DomainException(ErrorCode.ROLE_NOT_FOUND, "Role not found"))
            .getRoleId();
    }
}
