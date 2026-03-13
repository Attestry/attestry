package io.attestry.userauth.domain;

import io.attestry.commonlib.domain.exception.DomainException;

public class UserAuthDomainException extends DomainException {

    public UserAuthDomainException(UserAuthErrorCode errorCode) {
        super(errorCode);
    }

    public UserAuthDomainException(UserAuthErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public UserAuthDomainException(UserAuthErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
