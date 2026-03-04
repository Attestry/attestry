package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.application.port.OrganizationApplicationRepositoryPort;
import io.attestry.userauth.domain.onboarding.model.OrganizationApplication;
import io.attestry.userauth.domain.onboarding.repository.OrganizationApplicationRepository;
import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.OrganizationApplicationJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.OrganizationApplicationJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaOrganizationApplicationRepositoryAdapter implements OrganizationApplicationRepositoryPort, OrganizationApplicationRepository {

    private final OrganizationApplicationJpaRepository repository;

    public JpaOrganizationApplicationRepositoryAdapter(OrganizationApplicationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrganizationApplication save(OrganizationApplication application) {
        long rowVersion = repository.findById(application.applicationId())
            .map(OrganizationApplicationJpaEntity::getRowVersion)
            .orElse(0L);
        return toDomain(repository.save(toEntity(application, rowVersion)));
    }

    @Override
    public Optional<OrganizationApplication> findById(String applicationId) {
        return repository.findById(applicationId).map(this::toDomain);
    }

    @Override
    public Optional<OrganizationApplication> findByIdAndApplicantUserId(String applicationId, String applicantUserId) {
        return repository.findByApplicationIdAndApplicantUserId(applicationId, applicantUserId).map(this::toDomain);
    }

    @Override
    public List<OrganizationApplication> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<OrganizationApplication> findByType(GroupType type) {
        return repository.findByType(type).stream().map(this::toDomain).toList();
    }

    @Override
    public List<OrganizationApplication> findByApplicantUserId(String applicantUserId) {
        return repository.findByApplicantUserId(applicantUserId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<OrganizationApplication> findByTenantAndType(String tenantId, GroupType type) {
        return repository.findByTenantIdAndType(tenantId, type).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsBrandByOrgName(String orgName) {
        return repository.existsByTypeAndTenantIdIsNullAndOrgNameIgnoreCase(GroupType.BRAND, orgName);
    }

    @Override
    public boolean existsRetailByTenantAndOrgName(String tenantId, String orgName) {
        return repository.existsByTypeAndTenantIdAndOrgNameIgnoreCase(GroupType.RETAIL, tenantId, orgName);
    }

    @Override
    public boolean existsBrandByBizRegNo(String bizRegNo) {
        return repository.existsByTypeAndTenantIdIsNullAndBizRegNoIgnoreCase(GroupType.BRAND, bizRegNo);
    }

    @Override
    public boolean existsRetailByTenantAndBizRegNo(String tenantId, String bizRegNo) {
        return repository.existsByTypeAndTenantIdAndBizRegNoIgnoreCase(GroupType.RETAIL, tenantId, bizRegNo);
    }

    private OrganizationApplication toDomain(OrganizationApplicationJpaEntity entity) {
        return OrganizationApplication.reconstitute(
            entity.getApplicationId(),
            entity.getType(),
            entity.getApplicantUserId(),
            entity.getTenantId(),
            entity.getOrgName(),
            entity.getCountry(),
            entity.getBizRegNo(),
            entity.getEvidenceBundleId(),
            entity.getStatus(),
            entity.getReviewedByAdminId(),
            entity.getReviewedAt(),
            entity.getRejectReason()
        );
    }

    private OrganizationApplicationJpaEntity toEntity(OrganizationApplication domain, long rowVersion) {
        return new OrganizationApplicationJpaEntity(
            domain.applicationId(),
            domain.type(),
            domain.applicantUserId(),
            domain.tenantId(),
            domain.orgName(),
            domain.country(),
            domain.bizRegNo(),
            domain.evidenceBundleId(),
            domain.status(),
            domain.reviewedByAdminId(),
            domain.reviewedAt(),
            domain.rejectReason(),
            rowVersion
        );
    }
}
