package io.attestry.userauth.infrastructure.persistence.jpa.repository;

import io.attestry.userauth.domain.onboarding.model.ApplicationStatus;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.OrganizationApplicationJpaEntity;
import io.attestry.userauth.domain.organization.model.TenantType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationApplicationJpaRepository extends JpaRepository<OrganizationApplicationJpaEntity, String> {
    List<OrganizationApplicationJpaEntity> findByType(TenantType type);

    List<OrganizationApplicationJpaEntity> findByApplicantUserId(String applicantUserId);

    java.util.Optional<OrganizationApplicationJpaEntity> findByApplicationIdAndApplicantUserId(String applicationId, String applicantUserId);

    List<OrganizationApplicationJpaEntity> findByTenantId(String tenantId);

    List<OrganizationApplicationJpaEntity> findByTenantIdAndType(String tenantId, TenantType type);

    boolean existsByTypeAndTenantIdIsNullAndOrgNameIgnoreCaseAndCountryIgnoreCaseAndStatusNot(TenantType type, String orgName, String country, ApplicationStatus status);

    boolean existsByTypeAndTenantIdAndOrgNameIgnoreCaseAndCountryIgnoreCaseAndStatusNot(TenantType type, String tenantId, String orgName, String country, ApplicationStatus status);

    boolean existsByTypeAndTenantIdIsNullAndBizRegNoIgnoreCaseAndStatusNot(TenantType type, String bizRegNo, ApplicationStatus status);

    boolean existsByTypeAndTenantIdAndBizRegNoIgnoreCaseAndStatusNot(TenantType type, String tenantId, String bizRegNo, ApplicationStatus status);
}
