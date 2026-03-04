package io.attestry.product.domain;

public class ProductDomainException extends RuntimeException {

    private final ProductErrorCode errorCode;

    public ProductDomainException(ProductErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ProductErrorCode getErrorCode() {
        return errorCode;
    }
}
