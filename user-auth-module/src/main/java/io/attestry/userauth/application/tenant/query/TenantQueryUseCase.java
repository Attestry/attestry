package io.attestry.userauth.application.tenant.query;

import java.util.List;
import java.util.Map;

public interface TenantQueryUseCase {

    TenantView getTenant(String tenantId);

    Map<String, TenantView> getTenants(List<String> tenantIds);

    TenantPageView listTenants(String type, String status, String name, int page, int size);
}
