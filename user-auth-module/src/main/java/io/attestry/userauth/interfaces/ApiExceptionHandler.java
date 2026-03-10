package io.attestry.userauth.interfaces;

import io.attestry.commonlib.domain.exception.DomainException;
import io.attestry.commonlib.domain.exception.ErrorCategory;
import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.commonlib.infrastructure.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "io.attestry.userauth.interfaces")
public class ApiExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomain(DomainException ex) {
        ErrorCategory category = ex.getErrorCode().getCategory();
        HttpStatus status = switch (category) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case GONE -> HttpStatus.GONE;
            case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
        };
        ErrorResponse error = ErrorResponse.from(ex);
        return ResponseEntity.status(status).body(ApiResponse.error(error));
    }
}
