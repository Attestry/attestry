package io.attestry.userauth.interfaces.tenant.dto.response;

import io.attestry.userauth.application.dto.result.TenantPageResult;
import java.util.List;

public record TenantPageResponse(
        List<TenantResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static TenantPageResponse from(TenantPageResult result) {
        return new TenantPageResponse(
                result.items().stream().map(TenantResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}
