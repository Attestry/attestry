package io.attestry.product.interfaces.http;

import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "io.attestry.product.interfaces.http")
public class ProductApiExceptionHandler {

    @ExceptionHandler(ProductDomainException.class)
    public ResponseEntity<ErrorResponse> handle(ProductDomainException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case FORBIDDEN_MINT, FORBIDDEN_VOID, FORBIDDEN_RISK_FLAG,
                 TENANT_OR_GROUP_INACTIVE, MINT_CONTEXT_NOT_FOUND -> HttpStatus.FORBIDDEN;
            case ASSET_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case GENESIS_ALREADY_EXISTS, DUPLICATE_SERIAL_NUMBER,
                 ASSET_ALREADY_VOIDED, RISK_FLAG_ALREADY_SET, RISK_FLAG_NOT_SET -> HttpStatus.CONFLICT;
            case NOT_ASSET_OWNER -> HttpStatus.FORBIDDEN;
            case OUTBOX_ENQUEUE_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(new ErrorResponse(ex.getErrorCode().name(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(ProductErrorCode.OUTBOX_ENQUEUE_FAILED.name(), ex.getMessage()));
    }

    public record ErrorResponse(String code, String message) {
    }
}
