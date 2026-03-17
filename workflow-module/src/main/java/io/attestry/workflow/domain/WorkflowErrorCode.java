package io.attestry.workflow.domain;

import io.attestry.commonlib.domain.exception.ErrorCategory;
import io.attestry.commonlib.domain.exception.ErrorCode;

public enum WorkflowErrorCode implements ErrorCode {

    // Delegation
    DELEGATION_NOT_FOUND("DELEGATION", "Delegation not found", ErrorCategory.NOT_FOUND),
    DELEGATION_ALREADY_ACTIVE("DELEGATION", "Delegation already active", ErrorCategory.CONFLICT),
    DELEGATION_INVALID_STATE("DELEGATION", "Delegation invalid state", ErrorCategory.BAD_REQUEST),

    // Partner Link
    PARTNER_LINK_NOT_FOUND("PARTNER", "Partner link not found", ErrorCategory.NOT_FOUND),
    PARTNER_LINK_ALREADY_ACTIVE("PARTNER", "Partner link already active", ErrorCategory.BAD_REQUEST),
    PARTNER_LINK_DUPLICATE_STATUS("PARTNER", "Partner link duplicate status", ErrorCategory.CONFLICT),
    PARTNER_LINK_INVALID_STATE("PARTNER", "Partner link invalid state", ErrorCategory.BAD_REQUEST),

    // Transfer
    TRANSFER_NOT_FOUND("TRANSFER", "Transfer not found", ErrorCategory.NOT_FOUND),
    TRANSFER_INVALID_STATE("TRANSFER", "Transfer invalid state", ErrorCategory.BAD_REQUEST),
    TRANSFER_EXPIRED("TRANSFER", "Transfer expired", ErrorCategory.GONE),
    TRANSFER_BRUTE_FORCE_BLOCKED("TRANSFER", "Transfer brute force blocked", ErrorCategory.TOO_MANY_REQUESTS),
    TRANSFER_NONCE_MISMATCH("TRANSFER", "Transfer nonce mismatch", ErrorCategory.BAD_REQUEST),
    TRANSFER_CODE_MISMATCH("TRANSFER", "Transfer code mismatch", ErrorCategory.BAD_REQUEST),
    TRANSFER_ALREADY_PENDING("TRANSFER", "Transfer already pending", ErrorCategory.CONFLICT),

    // Claim
    CLAIM_NOT_FOUND("CLAIM", "Claim not found", ErrorCategory.NOT_FOUND),
    CLAIM_INVALID_STATE("CLAIM", "Claim invalid state", ErrorCategory.BAD_REQUEST),
    CLAIM_EVIDENCE_INSUFFICIENT("CLAIM", "Claim evidence insufficient", ErrorCategory.BAD_REQUEST),

    // Service Request
    SERVICE_REQUEST_NOT_FOUND("SERVICE_REQUEST", "Service request not found", ErrorCategory.NOT_FOUND),
    SERVICE_REQUEST_INVALID_STATE("SERVICE_REQUEST", "Service request invalid state", ErrorCategory.BAD_REQUEST),
    SERVICE_REQUEST_ALREADY_SUBMITTED("SERVICE_REQUEST", "이미 처리 중인 서비스 요청이 있습니다.", ErrorCategory.CONFLICT),

    // Distribution
    DISTRIBUTION_NOT_FOUND("DISTRIBUTION", "Distribution not found", ErrorCategory.NOT_FOUND),

    // Common
    EVIDENCE_NOT_FOUND("COMMON", "Evidence not found", ErrorCategory.NOT_FOUND),
    INVALID_REQUEST("COMMON", "Invalid request", ErrorCategory.BAD_REQUEST),
    INVALID_STATE("COMMON", "Invalid state", ErrorCategory.BAD_REQUEST),
    TENANT_ISOLATION_VIOLATION("COMMON", "Tenant isolation violation", ErrorCategory.FORBIDDEN),
    FORBIDDEN_SCOPE("COMMON", "Forbidden scope", ErrorCategory.FORBIDDEN);

    private final String group;
    private final String message;
    private final ErrorCategory category;

    WorkflowErrorCode(String group, String message, ErrorCategory category) {
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
