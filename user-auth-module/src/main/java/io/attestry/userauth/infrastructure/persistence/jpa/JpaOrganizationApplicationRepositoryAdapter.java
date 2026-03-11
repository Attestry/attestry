package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.application.port.OrganizationApplicationRepositoryPort;
import io.attestry.userauth.domain.onboarding.model.ApplicationStatus;
import io.attestry.userauth.domain.onboarding.model.OrganizationApplication;
import io.attestry.userauth.domain.onboarding.repository.OrganizationApplicationRepository;
import io.attestry.userauth.domain.organization.model.TenantType;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.OrganizationApplicationJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.OrganizationApplicationJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaOrganizationApplicationRepositoryAdapter
        implements OrganizationApplicationRepositoryPort, OrganizationApplicationRepository {

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
    public List<OrganizationApplication> findByType(TenantType type) {
        return repository.findByType(type).stream().map(this::toDomain).toList();
    }

    @Override
    public List<OrganizationApplication> findByApplicantUserId(String applicantUserId) {
        return repository.findByApplicantUserId(applicantUserId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<OrganizationApplication> findByTenantAndType(String tenantId, TenantType type) {
        return repository.findByTenantIdAndType(tenantId, type).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsBrandByOrgNameAndCountry(String orgName, String country) {
        return repository.existsByTypeAndTenantIdIsNullAndOrgNameIgnoreCaseAndCountryIgnoreCaseAndStatusNot(
                TenantType.BRAND, orgName, country, ApplicationStatus.REJECTED);
    }

    @Override
    public boolean existsRetailByTenantAndOrgNameAndCountry(String tenantId, String orgName, String country) {
        return repository.existsByTypeAndTenantIdAndOrgNameIgnoreCaseAndCountryIgnoreCaseAndStatusNot(
                TenantType.RETAIL, tenantId, orgName, country, ApplicationStatus.REJECTED);
    }

    @Override
    public boolean existsServiceByTenantAndOrgNameAndCountry(String tenantId, String orgName, String country) {
        return repository.existsByTypeAndTenantIdAndOrgNameIgnoreCaseAndCountryIgnoreCaseAndStatusNot(
                TenantType.SERVICE, tenantId, orgName, country, ApplicationStatus.REJECTED);
    }

    @Override
    public boolean existsBrandByBizRegNo(String bizRegNo) {
        return repository.existsByTypeAndTenantIdIsNullAndBizRegNoIgnoreCaseAndStatusNot(
                TenantType.BRAND, bizRegNo, ApplicationStatus.REJECTED);
    }

    @Override
    public boolean existsRetailByTenantAndBizRegNo(String tenantId, String bizRegNo) {
        return repository.existsByTypeAndTenantIdAndBizRegNoIgnoreCaseAndStatusNot(
                TenantType.RETAIL, tenantId, bizRegNo, ApplicationStatus.REJECTED);
    }

    @Override
    public boolean existsServiceByTenantAndBizRegNo(String tenantId, String bizRegNo) {
        return repository.existsByTypeAndTenantIdAndBizRegNoIgnoreCaseAndStatusNot(
                TenantType.SERVICE, tenantId, bizRegNo, ApplicationStatus.REJECTED);
    }

    private OrganizationApplication toDomain(OrganizationApplicationJpaEntity entity) {
        return OrganizationApplication.reconstitute(
                entity.getApplicationId(),
                entity.getType(),
                entity.getApplicantUserId(),
                entity.getTenantId(),
                entity.getOrgName(),
                entity.getCountry(),
                entity.getAddress(),
                entity.getBizRegNo(),
                entity.getEvidenceBundleId(),
                entity.getStatus(),
                entity.getReviewedByAdminId(),
                entity.getReviewedAt(),
                entity.getRejectReason());
    }

    private OrganizationApplicationJpaEntity toEntity(OrganizationApplication domain, long rowVersion) {
        return new OrganizationApplicationJpaEntity(
                domain.applicationId(),
                domain.type(),
                domain.applicantUserId(),
                domain.tenantId(),
                domain.orgName(),
                domain.country(),
                domain.address(),
                domain.bizRegNo(),
                domain.evidenceBundleId(),
                domain.status(),
                domain.reviewedByAdminId(),
                domain.reviewedAt(),
                domain.rejectReason(),
                rowVersion);
    }
}
