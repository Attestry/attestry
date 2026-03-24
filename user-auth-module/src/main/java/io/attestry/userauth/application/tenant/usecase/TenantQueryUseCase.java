package io.attestry.userauth.application.tenant.usecase;

import io.attestry.userauth.application.tenant.view.TenantPageView;
import io.attestry.userauth.application.tenant.view.TenantView;
import java.util.List;
import java.util.Map;

public interface TenantQueryUseCase {

    TenantView getTenant(String tenantId);

    Map<String, TenantView> getTenants(List<String> tenantIds);

    TenantPageView listTenants(String type, String status, String name, int page, int size);
}
