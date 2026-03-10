package io.attestry.product.domain;

import io.attestry.commonlib.domain.exception.DomainException;

public class ProductDomainException extends DomainException {

    public ProductDomainException(ProductErrorCode errorCode) {
        super(errorCode);
    }

    public ProductDomainException(ProductErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ProductDomainException(ProductErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
