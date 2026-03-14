package io.attestry.product.domain;

import io.attestry.commonlib.domain.exception.ErrorCategory;
import io.attestry.commonlib.domain.exception.ErrorCode;

public enum ProductErrorCode implements ErrorCode {

    // Mint
    INVALID_REQUEST("MINT", "Invalid request", ErrorCategory.BAD_REQUEST),
    FORBIDDEN_MINT("MINT", "Forbidden mint", ErrorCategory.FORBIDDEN),
    GENESIS_ALREADY_EXISTS("MINT", "Genesis already exists", ErrorCategory.CONFLICT),
    DUPLICATE_SERIAL_NUMBER("MINT", "Duplicate serial number", ErrorCategory.CONFLICT),
    TENANT_OR_GROUP_INACTIVE("MINT", "Tenant or group inactive", ErrorCategory.FORBIDDEN),
    MINT_CONTEXT_NOT_FOUND("MINT", "Mint context not found", ErrorCategory.FORBIDDEN),
    OUTBOX_ENQUEUE_FAILED("MINT", "Outbox enqueue failed", ErrorCategory.INTERNAL),

    // Void
    ASSET_ALREADY_VOIDED("VOID", "Asset already voided", ErrorCategory.CONFLICT),
    FORBIDDEN_VOID("VOID", "Forbidden void", ErrorCategory.FORBIDDEN),
    ASSET_NOT_FOUND("VOID", "Asset not found", ErrorCategory.NOT_FOUND),

    // Risk
    RISK_FLAG_ALREADY_SET("RISK", "Risk flag already set", ErrorCategory.CONFLICT),
    FORBIDDEN_RISK_FLAG("RISK", "Forbidden risk flag", ErrorCategory.FORBIDDEN),
    NOT_ASSET_OWNER("RISK", "Not asset owner", ErrorCategory.FORBIDDEN),
    RISK_FLAG_NOT_SET("RISK", "Risk flag not set", ErrorCategory.CONFLICT);

    private final String group;
    private final String message;
    private final ErrorCategory category;

    ProductErrorCode(String group, String message, ErrorCategory category) {
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
