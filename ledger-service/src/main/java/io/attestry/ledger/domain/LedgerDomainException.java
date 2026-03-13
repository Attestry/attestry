package io.attestry.ledger.domain;

public class LedgerDomainException extends RuntimeException {

    private final LedgerErrorCode errorCode;

    public LedgerDomainException(LedgerErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public LedgerErrorCode getErrorCode() {
        return errorCode;
    }
}
