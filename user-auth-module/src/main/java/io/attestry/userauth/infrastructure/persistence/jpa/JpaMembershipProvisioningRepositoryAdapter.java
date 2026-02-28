package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.application.port.MembershipProvisioningRepositoryPort;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipRoleAssignmentJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.MembershipRoleAssignmentJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.MembershipJpaRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaMembershipProvisioningRepositoryAdapter implements MembershipProvisioningRepositoryPort {

    private final MembershipJpaRepository repository;
    private final MembershipRoleAssignmentJpaRepository membershipRoleAssignmentRepository;

    public JpaMembershipProvisioningRepositoryAdapter(
        MembershipJpaRepository repository,
        MembershipRoleAssignmentJpaRepository membershipRoleAssignmentRepository
    ) {
        this.repository = repository;
        this.membershipRoleAssignmentRepository = membershipRoleAssignmentRepository;
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
            membershipRoleAssignmentRepository.save(new MembershipRoleAssignmentJpaEntity(
                UUID.randomUUID().toString(),
                saved.getMembershipId(),
                DefaultRoleIdMapper.map(saved.getRole(), saved.getGroupType()),
                null,
                Instant.now()
            ));
        }

        return new Membership(
            saved.getMembershipId(),
            saved.getUserId(),
            saved.getGroupId(),
            saved.getTenantId(),
            saved.getGroupType(),
            saved.getRole(),
            saved.getStatus(),
            saved.getGroupStatus(),
            saved.getTenantStatus()
        );
    }
}
