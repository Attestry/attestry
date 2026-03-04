package io.attestry.workflow.interfaces;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "io.attestry.workflow.interfaces")
public class WorkflowApiExceptionHandler {

    @ExceptionHandler(WorkflowDomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(WorkflowDomainException ex) {
        HttpStatus status = resolveStatus(ex.getErrorCode());
        return ResponseEntity.status(status)
            .body(new ErrorResponse(ex.getErrorCode().name(), ex.getMessage()));
    }

    private HttpStatus resolveStatus(WorkflowErrorCode code) {
        return switch (code) {
            case DELEGATION_NOT_FOUND, PARTNER_LINK_NOT_FOUND, EVIDENCE_NOT_FOUND, TRANSFER_NOT_FOUND, CLAIM_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case TENANT_ISOLATION_VIOLATION, FORBIDDEN_SCOPE -> HttpStatus.FORBIDDEN;
            case TRANSFER_EXPIRED -> HttpStatus.GONE;
            case TRANSFER_BRUTE_FORCE_BLOCKED -> HttpStatus.TOO_MANY_REQUESTS;
            case TRANSFER_ALREADY_PENDING -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    public record ErrorResponse(String code, String message) {
    }
}
