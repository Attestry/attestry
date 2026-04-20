package io.attestry.userauth.interfaces.tenant;

import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.application.tenant.query.TenantPageView;
import io.attestry.userauth.application.tenant.query.TenantQueryUseCase;
import io.attestry.userauth.application.tenant.query.TenantView;
import io.attestry.userauth.interfaces.tenant.dto.response.TenantPageResponse;
import io.attestry.userauth.interfaces.tenant.dto.response.TenantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tenants")
public class TenantManagementHttp {

    private final TenantQueryUseCase tenantQueryUseCase;

    @GetMapping("/{tenantId}")
    public ApiResponse<TenantResponse> getTenant(@PathVariable("tenantId") String tenantId) {
        TenantView result = tenantQueryUseCase.getTenant(tenantId);
        return ApiResponse.success(TenantResponse.from(result));
    }

    @GetMapping
    public ApiResponse<TenantPageResponse> listTenants(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        TenantPageView result = tenantQueryUseCase.listTenants(type, status, name, page, size);
        return ApiResponse.success(TenantPageResponse.from(result));
    }
}
