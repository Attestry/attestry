package io.attestry.userauth.application.dto.result;

public record GroupAdminResult(
    String groupId,
    String tenantId,
    String type,
    String status
) {
}
