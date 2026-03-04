package io.attestry.userauth.application.port;

import io.attestry.userauth.domain.onboarding.model.OrganizationApplication;
import io.attestry.userauth.domain.organization.model.GroupType;
import java.util.List;
import java.util.Optional;

public interface OrganizationApplicationRepositoryPort {
    OrganizationApplication save(OrganizationApplication application);

    Optional<OrganizationApplication> findById(String applicationId);

    Optional<OrganizationApplication> findByIdAndApplicantUserId(String applicationId, String applicantUserId);

    List<OrganizationApplication> findAll();

    List<OrganizationApplication> findByType(GroupType type);

    List<OrganizationApplication> findByApplicantUserId(String applicantUserId);

    List<OrganizationApplication> findByTenantAndType(String tenantId, GroupType type);

    boolean existsBrandByOrgName(String orgName);

    boolean existsRetailByTenantAndOrgName(String tenantId, String orgName);

    boolean existsBrandByBizRegNo(String bizRegNo);

    boolean existsRetailByTenantAndBizRegNo(String tenantId, String bizRegNo);
}
