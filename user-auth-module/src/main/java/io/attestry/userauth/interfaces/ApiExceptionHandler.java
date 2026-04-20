package io.attestry.userauth.interfaces;

import io.attestry.commonlib.domain.exception.DomainException;
import io.attestry.commonlib.domain.exception.ErrorCategory;
import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.commonlib.infrastructure.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        BindException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception ex) {
        String message = switch (ex) {
            case MethodArgumentNotValidException valid ->
                valid.getBindingResult().getFieldErrors().stream()
                    .map(fieldError -> fieldError.getDefaultMessage())
                    .filter(m -> m != null && !m.isBlank())
                    .findFirst()
                    .orElse("Please check your input.");
            case BindException bind ->
                bind.getBindingResult().getFieldErrors().stream()
                    .map(fieldError -> fieldError.getDefaultMessage())
                    .filter(m -> m != null && !m.isBlank())
                    .findFirst()
                    .orElse("Please check your input.");
            case ConstraintViolationException violation ->
                violation.getConstraintViolations().stream()
                    .map(v -> v.getMessage())
                    .filter(m -> m != null && !m.isBlank())
                    .findFirst()
                    .orElse("Please check your input.");
            case HttpMessageNotReadableException ignored -> "Invalid request body format.";
            default -> "Please check your input.";
        };

        ErrorResponse error = new ErrorResponse("INVALID_INPUT", message, "VALIDATION");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }
}
