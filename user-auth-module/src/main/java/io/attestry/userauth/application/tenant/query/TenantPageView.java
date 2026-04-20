package io.attestry.userauth.application.tenant.query;

import java.util.List;

public record TenantPageView(
        List<TenantView> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
