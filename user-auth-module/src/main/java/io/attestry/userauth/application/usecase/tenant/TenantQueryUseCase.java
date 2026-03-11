package io.attestry.userauth.application.usecase.tenant;

import io.attestry.userauth.application.dto.result.TenantPageResult;
import io.attestry.userauth.application.dto.result.TenantResult;

public interface TenantQueryUseCase {

    TenantResult getTenant(String tenantId);

    TenantPageResult listTenants(String type, String status, String name, int page, int size);
}
