package io.attestry.workflow.interfaces.transfer;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.transfer.command.AcceptTransferCommand;
import io.attestry.workflow.application.transfer.command.CreateB2CTransferCommand;
import io.attestry.workflow.application.transfer.command.CreateC2CTransferCommand;
import io.attestry.workflow.application.transfer.result.AcceptTransferResult;
import io.attestry.workflow.application.transfer.result.CancelTransferResult;
import io.attestry.workflow.application.transfer.result.CreateTransferResult;
import io.attestry.workflow.application.usecase.TransferAcceptUseCase;
import io.attestry.workflow.application.usecase.TransferCancelUseCase;
import io.attestry.workflow.application.usecase.TransferCreateUseCase;
import io.attestry.workflow.domain.transfer.model.AcceptMethod;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflows")
public class TransferHttp {

    private final TransferCreateUseCase transferCreateUseCase;
    private final TransferAcceptUseCase transferAcceptUseCase;
    private final TransferCancelUseCase transferCancelUseCase;

    public TransferHttp(
        TransferCreateUseCase transferCreateUseCase,
        TransferAcceptUseCase transferAcceptUseCase,
        TransferCancelUseCase transferCancelUseCase
    ) {
        this.transferCreateUseCase = transferCreateUseCase;
        this.transferAcceptUseCase = transferAcceptUseCase;
        this.transferCancelUseCase = transferCancelUseCase;
    }

    @PostMapping("/passports/{passportId}/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_OWNER_TRANSFER_CREATE')")
    public CreateTransferResponse createC2C(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("passportId") String passportId,
        @RequestBody CreateTransferRequest request
    ) {
        CreateTransferResult result = transferCreateUseCase.createC2C(
            principal,
            passportId,
            new CreateC2CTransferCommand(request.acceptMethod(), request.password(), request.expiresAt())
        );
        return CreateTransferResponse.from(result);
    }

    @PostMapping("/tenants/{tenantId}/passports/{passportId}/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_RETAIL_TRANSFER_CREATE')")
    public CreateTransferResponse createB2C(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("passportId") String passportId,
        @RequestBody CreateTransferRequest request
    ) {
        CreateTransferResult result = transferCreateUseCase.createB2C(
            principal,
            tenantId,
            passportId,
            new CreateB2CTransferCommand(request.acceptMethod(), request.password(), request.expiresAt())
        );
        return CreateTransferResponse.from(result);
    }

    @PostMapping("/transfers/{transferId}/accept")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('SCOPE_OWNER_TRANSFER_ACCEPT')")
    public AcceptTransferResponse accept(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("transferId") String transferId,
        @RequestBody AcceptTransferRequest request
    ) {
        AcceptTransferResult result = transferAcceptUseCase.accept(
            principal,
            transferId,
            new AcceptTransferCommand(request.qrNonce(), request.password())
        );
        return AcceptTransferResponse.from(result);
    }

    @PostMapping("/transfers/{transferId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('SCOPE_OWNER_TRANSFER_CREATE')")
    public CancelTransferResponse cancel(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("transferId") String transferId
    ) {
        CancelTransferResult result = transferCancelUseCase.cancel(principal, transferId);
        return CancelTransferResponse.from(result);
    }

    public record CreateTransferRequest(
        AcceptMethod acceptMethod,
        String password,
        Instant expiresAt
    ) {
    }

    public record AcceptTransferRequest(
        String qrNonce,
        String password
    ) {
    }

    public record CreateTransferResponse(
        String transferId,
        String passportId,
        String transferType,
        String status,
        String acceptMethod,
        String qrNonce,
        Instant expiresAt
    ) {
        static CreateTransferResponse from(CreateTransferResult result) {
            return new CreateTransferResponse(
                result.transferId(), result.passportId(),
                result.transferType(), result.status(),
                result.acceptMethod(), result.qrNonce(), result.expiresAt()
            );
        }
    }

    public record AcceptTransferResponse(
        String transferId,
        String passportId,
        String status,
        String toOwnerId,
        Instant completedAt,
        String outboxEventId
    ) {
        static AcceptTransferResponse from(AcceptTransferResult result) {
            return new AcceptTransferResponse(
                result.transferId(), result.passportId(),
                result.status(), result.toOwnerId(),
                result.completedAt(), result.outboxEventId()
            );
        }
    }

    public record CancelTransferResponse(
        String transferId,
        String passportId,
        String status,
        Instant cancelledAt
    ) {
        static CancelTransferResponse from(CancelTransferResult result) {
            return new CancelTransferResponse(
                result.transferId(), result.passportId(),
                result.status(), result.cancelledAt()
            );
        }
    }
}
