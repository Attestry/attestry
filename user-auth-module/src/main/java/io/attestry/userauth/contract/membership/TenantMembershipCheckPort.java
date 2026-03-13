package io.attestry.userauth.contract.membership;

public interface TenantMembershipCheckPort {

    MembershipCheckResult checkActiveMembership(String userId, String tenantId, String tenantType);

    record MembershipCheckResult(
        boolean active,
        String tenantId,
        String tenantType
    ) {
    }
}
