package io.attestry.userauth.interfaces.membership.dto.response;

import io.attestry.userauth.application.dto.result.GroupAdminResult;

public record GroupResponse(
        String groupId,
        String tenantId,
        String type,
        String status
) {
    public static GroupResponse from(GroupAdminResult group) {
        return new GroupResponse(group.groupId(), group.tenantId(), group.type(), group.status());
    }
}