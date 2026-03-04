package io.attestry.userauth.domain.membership.model;

import java.time.Instant;

public record RoleAssignment(String roleCode, String assignedByUserId, Instant assignedAt) {
}
