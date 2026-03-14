package io.attestry.userauth.domain;

import io.attestry.commonlib.domain.exception.ErrorCategory;
import io.attestry.commonlib.domain.exception.ErrorCode;

public enum UserAuthErrorCode implements ErrorCode {

    // Identity
    DUPLICATE_EMAIL("IDENTITY", "Email already exists", ErrorCategory.CONFLICT),
    USER_NOT_FOUND("IDENTITY", "User not found", ErrorCategory.UNAUTHORIZED),
    INVALID_CREDENTIALS("IDENTITY", "Invalid credentials", ErrorCategory.UNAUTHORIZED),
    USER_SUSPENDED("IDENTITY", "User is suspended", ErrorCategory.UNAUTHORIZED),
    ACCESS_TOKEN_INVALID("IDENTITY", "Access token is invalid", ErrorCategory.UNAUTHORIZED),

    // Membership
    MEMBERSHIP_NOT_FOUND("MEMBERSHIP", "Membership not found", ErrorCategory.NOT_FOUND),
    DUPLICATE_MEMBERSHIP("MEMBERSHIP", "Duplicate membership", ErrorCategory.CONFLICT),
    ROLE_NOT_FOUND("MEMBERSHIP", "Role not found", ErrorCategory.NOT_FOUND),
    ROLE_ASSIGNMENT_NOT_FOUND("MEMBERSHIP", "Role assignment not found", ErrorCategory.NOT_FOUND),
    INVITATION_NOT_FOUND("MEMBERSHIP", "Invitation not found", ErrorCategory.NOT_FOUND),
    GROUP_NOT_FOUND("MEMBERSHIP", "Group not found", ErrorCategory.NOT_FOUND),

    // Onboarding
    APPLICATION_NOT_FOUND("ONBOARDING", "Application not found", ErrorCategory.NOT_FOUND),
    INVALID_APPLICATION_STATE("ONBOARDING", "Invalid application state", ErrorCategory.CONFLICT),
    DUPLICATE_ORGANIZATION_NAME("ONBOARDING", "Organization name already exists", ErrorCategory.CONFLICT),
    DUPLICATE_BIZ_REG_NO("ONBOARDING", "Business registration number already exists", ErrorCategory.CONFLICT),
    EVIDENCE_NOT_FOUND("ONBOARDING", "Evidence not found", ErrorCategory.NOT_FOUND),
    EVIDENCE_FILE_LIMIT_EXCEEDED("ONBOARDING", "Evidence file limit exceeded", ErrorCategory.BAD_REQUEST),
    EVIDENCE_FILE_TYPE_NOT_ALLOWED("ONBOARDING", "Evidence file type not allowed", ErrorCategory.BAD_REQUEST),

    // Organization
    TENANT_NOT_FOUND("ORGANIZATION", "Tenant not found", ErrorCategory.NOT_FOUND),
    TENANT_ISOLATION_VIOLATION("ORGANIZATION", "Tenant isolation violation", ErrorCategory.FORBIDDEN),

    // Authorization
    FORBIDDEN_SCOPE("AUTHORIZATION", "Forbidden scope", ErrorCategory.FORBIDDEN),
    TEMPLATE_NOT_FOUND("AUTHORIZATION", "Template not found", ErrorCategory.NOT_FOUND),
    PERMISSION_NOT_FOUND("AUTHORIZATION", "Permission not found", ErrorCategory.NOT_FOUND),
    TENANT_ROLE_TEMPLATE_BINDING_NOT_FOUND("AUTHORIZATION", "Tenant role template binding not found", ErrorCategory.NOT_FOUND),
    DUPLICATE_TEMPLATE_CODE("AUTHORIZATION", "Duplicate template code", ErrorCategory.CONFLICT),

    // Common
    INVALID_REQUEST("COMMON", "Invalid request", ErrorCategory.BAD_REQUEST);

    private final String group;
    private final String message;
    private final ErrorCategory category;

    UserAuthErrorCode(String group, String message, ErrorCategory category) {
        this.group = group;
        this.message = message;
        this.category = category;
    }

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ErrorCategory getCategory() {
        return category;
    }
}
