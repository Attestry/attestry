package io.attestry.userauth.domain.membership.model;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.membership.event.RoleAssignmentAuditedEvent;
import io.attestry.userauth.domain.membership.service.RoleAssignmentDomainService;
import io.attestry.userauth.domain.organization.model.GroupStatus;
import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.organization.model.TenantStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Membership {

    private final String membershipId;
    private final String userId;
    private final String groupId;
    private final String tenantId;
    private final GroupType groupType;
    private MembershipRole role;
    private MembershipStatus status;
    private GroupStatus groupStatus;
    private TenantStatus tenantStatus;
    private final Set<RoleAssignment> roleAssignments;
    private final List<Object> domainEvents = new ArrayList<>();

    private Membership(
        String membershipId, String userId, String groupId, String tenantId,
        GroupType groupType, MembershipRole role, MembershipStatus status,
        GroupStatus groupStatus, TenantStatus tenantStatus,
        Set<RoleAssignment> roleAssignments
    ) {
        this.membershipId = membershipId;
        this.userId = userId;
        this.groupId = groupId;
        this.tenantId = tenantId;
        this.groupType = groupType;
        this.role = role;
        this.status = status;
        this.groupStatus = groupStatus;
        this.tenantStatus = tenantStatus;
        this.roleAssignments = new HashSet<>(roleAssignments);
    }

    public static Membership create(
        String userId, String groupId, String tenantId,
        GroupType groupType, MembershipRole role,
        GroupStatus groupStatus, TenantStatus tenantStatus
    ) {
        return new Membership(
            UUID.randomUUID().toString(), userId, groupId, tenantId,
            groupType, role, MembershipStatus.ACTIVE,
            groupStatus, tenantStatus, Set.of()
        );
    }

    public static Membership reconstitute(
        String membershipId, String userId, String groupId, String tenantId,
        GroupType groupType, MembershipRole role, MembershipStatus status,
        GroupStatus groupStatus, TenantStatus tenantStatus,
        Set<RoleAssignment> roleAssignments
    ) {
        return new Membership(membershipId, userId, groupId, tenantId,
            groupType, role, status, groupStatus, tenantStatus, roleAssignments);
    }

    public boolean isActive() {
        return status == MembershipStatus.ACTIVE
            && groupStatus == GroupStatus.ACTIVE
            && tenantStatus == TenantStatus.ACTIVE;
    }

    public void assignRole(String roleCode, String assignedByUserId, Instant now,
                           RoleAssignmentDomainService roleService,
                           Set<String> actorRoleCodes, String actorMembershipId) {
        var eval = roleService.evaluate(actorRoleCodes, actorMembershipId, membershipId, roleCode);
        if (eval.denied()) {
            domainEvents.add(createAuditEvent(assignedByUserId, eval.normalizedRoleCode(), true,
                false, mapDenialReason(eval.denialReason()), now));
            throw new DomainException(mapDenialError(eval.denialReason()), mapDenialMessage(eval.denialReason()));
        }
        String normalized = eval.normalizedRoleCode();
        roleAssignments.removeIf(ra -> ra.roleCode().equals(normalized));
        roleAssignments.add(new RoleAssignment(normalized, assignedByUserId, now));
        domainEvents.add(createAuditEvent(assignedByUserId, normalized, true, true, null, now));
    }

    public void revokeRole(String roleCode, String actorUserId, Instant now,
                           RoleAssignmentDomainService roleService,
                           Set<String> actorRoleCodes, String actorMembershipId) {
        var eval = roleService.evaluate(actorRoleCodes, actorMembershipId, membershipId, roleCode);
        if (eval.denied()) {
            domainEvents.add(createAuditEvent(actorUserId, eval.normalizedRoleCode(), false,
                false, mapDenialReason(eval.denialReason()), now));
            throw new DomainException(mapDenialError(eval.denialReason()), mapDenialMessage(eval.denialReason()));
        }
        String normalized = eval.normalizedRoleCode();
        roleAssignments.removeIf(ra -> ra.roleCode().equals(normalized));
        domainEvents.add(createAuditEvent(actorUserId, normalized, false, true, null, now));
    }

    public Set<String> currentRoleCodes() {
        return roleAssignments.stream()
            .map(RoleAssignment::roleCode)
            .collect(Collectors.toUnmodifiableSet());
    }

    public void syncGroupStatus(GroupStatus newStatus) {
        this.groupStatus = newStatus;
    }

    public void updateStatus(MembershipStatus newStatus) {
        this.status = newStatus;
    }

    public List<Object> harvestEvents() {
        List<Object> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    private RoleAssignmentAuditedEvent createAuditEvent(
        String actorUserId, String roleCode, boolean assign,
        boolean allowed, String reasonCode, Instant now
    ) {
        return new RoleAssignmentAuditedEvent(
            actorUserId, tenantId, membershipId,
            assign ? null : roleCode,
            assign ? roleCode : null,
            "POLICY_CHECK",
            allowed, reasonCode, now, now
        );
    }

    private static ErrorCode mapDenialError(RoleAssignmentDomainService.DenialReason reason) {
        return switch (reason) {
            case INVALID_ROLE_CODE -> ErrorCode.ROLE_NOT_FOUND;
            case SELF_ESCALATION_DENIED, NOT_ASSIGNABLE -> ErrorCode.FORBIDDEN_SCOPE;
            default -> ErrorCode.FORBIDDEN_SCOPE;
        };
    }

    private static String mapDenialMessage(RoleAssignmentDomainService.DenialReason reason) {
        return switch (reason) {
            case INVALID_ROLE_CODE -> "Role code is required";
            case SELF_ESCALATION_DENIED -> "Self escalation is not allowed";
            case NOT_ASSIGNABLE -> "Role assignment is not allowed";
            default -> "Role assignment denied";
        };
    }

    private static String mapDenialReason(RoleAssignmentDomainService.DenialReason reason) {
        return switch (reason) {
            case NONE -> null;
            case INVALID_ROLE_CODE -> ErrorCode.ROLE_NOT_FOUND.name();
            case SELF_ESCALATION_DENIED, NOT_ASSIGNABLE -> ErrorCode.FORBIDDEN_SCOPE.name();
        };
    }

    // Getters
    public String membershipId() { return membershipId; }
    public String userId() { return userId; }
    public String groupId() { return groupId; }
    public String tenantId() { return tenantId; }
    public GroupType groupType() { return groupType; }
    public MembershipRole role() { return role; }
    public MembershipStatus status() { return status; }
    public GroupStatus groupStatus() { return groupStatus; }
    public TenantStatus tenantStatus() { return tenantStatus; }
    public Set<RoleAssignment> roleAssignments() { return Collections.unmodifiableSet(roleAssignments); }
}
