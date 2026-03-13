package io.attestry.userauth.application.usecase.tenant;

import io.attestry.userauth.application.dto.result.TenantPageResult;
import io.attestry.userauth.application.dto.result.TenantResult;
import java.util.List;
import java.util.Map;

public interface TenantQueryUseCase {

    TenantResult getTenant(String tenantId);

    Map<String, TenantResult> getTenants(List<String> tenantIds);

    TenantPageResult listTenants(String type, String status, String name, int page, int size);
}
