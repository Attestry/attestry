package io.attestry.userauth.infrastructure.persistence.jpa.onboarding;

import io.attestry.userauth.application.port.onboarding.OrganizationApplicationRepositoryPort;
import io.attestry.userauth.domain.onboarding.model.ApplicationStatus;
import io.attestry.userauth.domain.onboarding.model.OrganizationApplication;
import io.attestry.userauth.domain.tenant.model.TenantType;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.OrganizationApplicationJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.mapper.OrganizationApplicationMapper;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.OrganizationApplicationJpaRepository;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class JpaOrganizationApplicationRepositoryAdapter
        implements OrganizationApplicationRepositoryPort {

    private final OrganizationApplicationJpaRepository repository;
    private final OrganizationApplicationMapper mapper;


    @Override
    public OrganizationApplication save(OrganizationApplication application) {
        long rowVersion = repository.findById(application.applicationId())
                .map(OrganizationApplicationJpaEntity::getRowVersion)
                .orElse(0L);
        return mapper.toDomain(repository.save(mapper.toEntity(application, rowVersion)));
    }

    @Override
    public Optional<OrganizationApplication> findById(String applicationId) {
        return repository.findById(applicationId).map(mapper::toDomain);
    }

    @Override
    public Optional<OrganizationApplication> findByIdAndApplicantUserId(String applicationId, String applicantUserId) {
        return repository.findByApplicationIdAndApplicantUserId(applicationId, applicantUserId).map(mapper::toDomain);
    }

    @Override
    public List<OrganizationApplication> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<OrganizationApplication> findByType(TenantType type) {
        return repository.findByType(type).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<OrganizationApplication> findByApplicantUserId(String applicantUserId) {
        return repository.findByApplicantUserId(applicantUserId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<OrganizationApplication> findByTenantAndType(String tenantId, TenantType type) {
        return repository.findByTenantIdAndType(tenantId, type).stream().map(mapper::toDomain).toList();
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
}
