package io.attestry.commonlib.domain.exception;

public interface ErrorCode {

    String getCode();

    String getMessage();

    default String getGroup() {
        return "";
    }

    default ErrorCategory getCategory() {
        return ErrorCategory.BAD_REQUEST;
    }
}
