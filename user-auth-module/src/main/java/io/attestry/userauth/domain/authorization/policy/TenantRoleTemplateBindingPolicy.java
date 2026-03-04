package io.attestry.userauth.domain.authorization.policy;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.authorization.model.RoleCodes;
import java.util.Set;

public class TenantRoleTemplateBindingPolicy {

    private static final Set<String> ALLOWED_TENANT_ROLE_CODES = Set.of(
        RoleCodes.TENANT_OWNER,
        RoleCodes.TENANT_OPERATOR,
        RoleCodes.TENANT_STAFF
    );

    public void assertAllowedRoleCode(String normalizedRoleCode) {
        if (!ALLOWED_TENANT_ROLE_CODES.contains(normalizedRoleCode)) {
            throw new DomainException(
                ErrorCode.INVALID_REQUEST,
                "roleCode must be one of TENANT_OWNER, TENANT_OPERATOR, TENANT_STAFF"
            );
        }
    }
}
