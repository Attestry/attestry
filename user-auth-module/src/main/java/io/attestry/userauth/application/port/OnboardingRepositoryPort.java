package io.attestry.userauth.application.port;

import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.onboarding.model.OrganizationApplication;
import java.util.List;
import java.util.Optional;

public interface OnboardingRepositoryPort {
    OrganizationApplication saveApplication(OrganizationApplication application);

    Optional<OrganizationApplication> findApplicationById(String applicationId);

    List<OrganizationApplication> findApplicationsByType(GroupType type);

    List<OrganizationApplication> findApplicationsByTenantAndType(String tenantId, GroupType type);

    void createTenant(String tenantId, String name, String region);

    void createGroup(String groupId, String tenantId, GroupType type);

    void createMembershipAsAdmin(String membershipId, String userId, String groupId, String tenantId, GroupType groupType);
}
