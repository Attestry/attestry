package io.attestry.userauth.interfaces;

import io.attestry.userauth.common.error.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case USER_NOT_FOUND, INVALID_CREDENTIALS, ACCESS_TOKEN_INVALID -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN_SCOPE, TENANT_ISOLATION_VIOLATION -> HttpStatus.FORBIDDEN;
            case DUPLICATE_EMAIL -> HttpStatus.CONFLICT;
            case APPLICATION_NOT_FOUND, INVITATION_NOT_FOUND, MEMBERSHIP_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_APPLICATION_STATE -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(new ErrorResponse(ex.getErrorCode().name(), ex.getMessage()));
    }

    public record ErrorResponse(String code, String message) {
    }
}
