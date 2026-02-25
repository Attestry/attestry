package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.application.port.OnboardingRepositoryPort;
import io.attestry.userauth.domain.organization.model.GroupStatus;
import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.onboarding.model.OrganizationApplication;
import io.attestry.userauth.domain.organization.model.TenantStatus;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.GroupJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.OrganizationApplicationJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.TenantJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.GroupJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.MembershipJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.OrganizationApplicationJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.TenantJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaOnboardingRepositoryAdapter implements OnboardingRepositoryPort {

    private final OrganizationApplicationJpaRepository applicationRepository;
    private final TenantJpaRepository tenantRepository;
    private final GroupJpaRepository groupRepository;
    private final MembershipJpaRepository membershipRepository;

    public JpaOnboardingRepositoryAdapter(
        OrganizationApplicationJpaRepository applicationRepository,
        TenantJpaRepository tenantRepository,
        GroupJpaRepository groupRepository,
        MembershipJpaRepository membershipRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.tenantRepository = tenantRepository;
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    public OrganizationApplication saveApplication(OrganizationApplication application) {
        OrganizationApplicationJpaEntity saved = applicationRepository.save(toEntity(application));
        return toDomain(saved);
    }

    @Override
    public Optional<OrganizationApplication> findApplicationById(String applicationId) {
        return applicationRepository.findById(applicationId).map(this::toDomain);
    }

    @Override
    public List<OrganizationApplication> findApplicationsByType(GroupType type) {
        return applicationRepository.findByType(type).stream().map(this::toDomain).toList();
    }

    @Override
    public List<OrganizationApplication> findApplicationsByTenantAndType(String tenantId, GroupType type) {
        return applicationRepository.findByTenantIdAndType(tenantId, type).stream().map(this::toDomain).toList();
    }

    @Override
    public void createTenant(String tenantId, String name, String region) {
        tenantRepository.save(new TenantJpaEntity(tenantId, name, region, TenantStatus.ACTIVE));
    }

    @Override
    public void createGroup(String groupId, String tenantId, GroupType type) {
        groupRepository.save(new GroupJpaEntity(groupId, tenantId, type, GroupStatus.ACTIVE));
    }

    @Override
    public void createMembershipAsAdmin(String membershipId, String userId, String groupId, String tenantId, GroupType groupType) {
        membershipRepository.save(new MembershipJpaEntity(
            membershipId,
            userId,
            groupId,
            tenantId,
            groupType,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));
    }

    private OrganizationApplication toDomain(OrganizationApplicationJpaEntity entity) {
        return new OrganizationApplication(
            entity.getApplicationId(),
            entity.getType(),
            entity.getApplicantUserId(),
            entity.getTenantId(),
            entity.getOrgName(),
            entity.getCountry(),
            entity.getBizRegNo(),
            entity.getEvidenceGroupId(),
            entity.getStatus(),
            entity.getReviewedByAdminId(),
            entity.getReviewedAt(),
            entity.getRejectReason()
        );
    }

    private OrganizationApplicationJpaEntity toEntity(OrganizationApplication domain) {
        return new OrganizationApplicationJpaEntity(
            domain.applicationId(),
            domain.type(),
            domain.applicantUserId(),
            domain.tenantId(),
            domain.orgName(),
            domain.country(),
            domain.bizRegNo(),
            domain.evidenceGroupId(),
            domain.status(),
            domain.reviewedByAdminId(),
            domain.reviewedAt(),
            domain.rejectReason()
        );
    }
}
