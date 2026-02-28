package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.application.port.MembershipAdminRepositoryPort;
import io.attestry.userauth.domain.membership.model.Invitation;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
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
import io.attestry.userauth.infrastructure.persistence.jpa.repository.UserAccountJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaMembershipAdminRepositoryAdapter implements MembershipAdminRepositoryPort {

    private final InvitationJpaRepository invitationRepository;
    private final MembershipJpaRepository membershipRepository;
    private final MembershipRoleAssignmentJpaRepository membershipRoleAssignmentRepository;
    private final UserAccountJpaRepository userAccountRepository;
    private final GroupJpaRepository groupRepository;

    public JpaMembershipAdminRepositoryAdapter(
        InvitationJpaRepository invitationRepository,
        MembershipJpaRepository membershipRepository,
        MembershipRoleAssignmentJpaRepository membershipRoleAssignmentRepository,
        UserAccountJpaRepository userAccountRepository,
        GroupJpaRepository groupRepository
    ) {
        this.invitationRepository = invitationRepository;
        this.membershipRepository = membershipRepository;
        this.membershipRoleAssignmentRepository = membershipRoleAssignmentRepository;
        this.userAccountRepository = userAccountRepository;
        this.groupRepository = groupRepository;
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
        GroupView group = findGroupById(groupId).orElseThrow();
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
            DefaultRoleIdMapper.map(saved.getRole(), saved.getGroupType()),
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
    public Membership updateMembership(String membershipId, MembershipRole role, MembershipStatus status) {
        MembershipJpaEntity current = membershipRepository.findById(membershipId).orElseThrow();
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
        if (current.getRole() != role) {
            MembershipRoleAssignmentJpaEntity assignment = membershipRoleAssignmentRepository.findByMembershipId(saved.getMembershipId()).orElse(null);
            membershipRoleAssignmentRepository.save(new MembershipRoleAssignmentJpaEntity(
                assignment == null ? UUID.randomUUID().toString() : assignment.getAssignmentId(),
                saved.getMembershipId(),
                DefaultRoleIdMapper.map(saved.getRole(), saved.getGroupType()),
                null,
                Instant.now()
            ));
        }
        return toDomain(saved);
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
