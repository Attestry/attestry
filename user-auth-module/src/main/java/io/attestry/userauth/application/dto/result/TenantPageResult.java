package io.attestry.userauth.application.dto.result;

import java.util.List;

public record TenantPageResult(
        List<TenantResult> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
