package io.attestry.userauth.application.port;

import io.attestry.userauth.domain.onboarding.model.OrganizationApplication;
import io.attestry.userauth.domain.organization.model.TenantType;
import java.util.List;
import java.util.Optional;

public interface OrganizationApplicationRepositoryPort {
    OrganizationApplication save(OrganizationApplication application);

    Optional<OrganizationApplication> findById(String applicationId);

    Optional<OrganizationApplication> findByIdAndApplicantUserId(String applicationId, String applicantUserId);

    List<OrganizationApplication> findAll();

    List<OrganizationApplication> findByType(TenantType type);

    List<OrganizationApplication> findByApplicantUserId(String applicantUserId);

    List<OrganizationApplication> findByTenantAndType(String tenantId, TenantType type);

    boolean existsBrandByOrgNameAndCountry(String orgName, String country);

    boolean existsRetailByTenantAndOrgNameAndCountry(String tenantId, String orgName, String country);

    boolean existsBrandByBizRegNo(String bizRegNo);

    boolean existsRetailByTenantAndBizRegNo(String tenantId, String bizRegNo);
}
