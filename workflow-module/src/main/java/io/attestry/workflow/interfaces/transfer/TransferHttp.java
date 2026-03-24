package io.attestry.workflow.interfaces.transfer;

import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.transfer.command.AcceptTransferCommand;
import io.attestry.workflow.application.transfer.command.CreateB2CTransferCommand;
import io.attestry.workflow.application.transfer.command.CreateC2CTransferCommand;
import io.attestry.workflow.application.transfer.result.AcceptTransferResult;
import io.attestry.workflow.application.transfer.result.CancelTransferResult;
import io.attestry.workflow.application.transfer.result.CreateTransferResult;
import io.attestry.workflow.application.transfer.usecase.TransferAcceptUseCase;
import io.attestry.workflow.application.transfer.usecase.TransferCancelUseCase;
import io.attestry.workflow.application.transfer.usecase.TransferCreateUseCase;
import io.attestry.workflow.application.transfer.usecase.TransferQueryUseCase;
import io.attestry.workflow.application.transfer.view.PagedCompletedTransferView;
import io.attestry.workflow.interfaces.transfer.dto.request.AcceptTransferRequest;
import io.attestry.workflow.interfaces.transfer.dto.request.CreateTransferRequest;
import io.attestry.workflow.interfaces.transfer.dto.response.AcceptTransferResponse;
import io.attestry.workflow.interfaces.transfer.dto.response.CancelTransferResponse;
import io.attestry.workflow.interfaces.transfer.dto.response.CompletedTransferResponse;
import io.attestry.workflow.interfaces.transfer.dto.response.CreateTransferResponse;
import io.attestry.workflow.interfaces.transfer.dto.response.PagedCompletedTransferResponse;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/workflows")
public class TransferHttp {

    private final TransferCreateUseCase transferCreateUseCase;
    private final TransferAcceptUseCase transferAcceptUseCase;
    private final TransferCancelUseCase transferCancelUseCase;
    private final TransferQueryUseCase transferQueryUseCase;

    @PostMapping("/passports/{passportId}/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_OWNER_TRANSFER_CREATE')")
    public ApiResponse<CreateTransferResponse> createC2C(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("passportId") String passportId,
        @RequestBody CreateTransferRequest request
    ) {
        CreateTransferResult result = transferCreateUseCase.createC2C(
            actor(principal),
            passportId,
            new CreateC2CTransferCommand(request.acceptMethod(), request.password(), request.expiresAt())
        );
        return ApiResponse.success(CreateTransferResponse.from(result));
    }

    @PostMapping("/tenants/{tenantId}/passports/{passportId}/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_RETAIL_TRANSFER_CREATE')")
    public ApiResponse<CreateTransferResponse> createB2C(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("passportId") String passportId,
        @RequestBody CreateTransferRequest request
    ) {
        CreateTransferResult result = transferCreateUseCase.createB2C(
            actor(principal),
            tenantId,
            passportId,
            new CreateB2CTransferCommand(request.acceptMethod(), request.password(), request.expiresAt())
        );
        return ApiResponse.success(CreateTransferResponse.from(result));
    }

    @GetMapping("/passports/{passportId}/transfers/pending")
    @PreAuthorize("hasAuthority('SCOPE_OWNER_TRANSFER_CREATE')")
    public ResponseEntity<ApiResponse<CreateTransferResponse>> findLatestActivePending(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("passportId") String passportId
    ) {
        Optional<CreateTransferResult> result = transferCreateUseCase.findLatestActivePendingByPassportId(actor(principal), passportId);
        return result
            .map(CreateTransferResponse::from)
            .map(r -> ResponseEntity.ok(ApiResponse.success(r)))
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/tenants/{tenantId}/passports/{passportId}/transfers/pending")
    @PreAuthorize("hasAuthority('SCOPE_RETAIL_TRANSFER_CREATE')")
    public ResponseEntity<ApiResponse<CreateTransferResponse>> findLatestActivePendingB2C(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("passportId") String passportId
    ) {
        Optional<CreateTransferResult> result = transferCreateUseCase.findLatestActivePendingB2CByPassportId(
            actor(principal),
            tenantId,
            passportId
        );
        return result
            .map(CreateTransferResponse::from)
            .map(r -> ResponseEntity.ok(ApiResponse.success(r)))
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/tenants/{tenantId}/transfers/completed")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public ApiResponse<PagedCompletedTransferResponse> listCompletedB2CTransfers(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @org.springframework.web.bind.annotation.RequestParam(value = "sourceTenantId", required = false) String sourceTenantId,
        @org.springframework.web.bind.annotation.RequestParam(value = "page", defaultValue = "0") int page,
        @org.springframework.web.bind.annotation.RequestParam(value = "size", defaultValue = "20") int size
    ) {
        PagedCompletedTransferView result = transferQueryUseCase.listCompletedB2CTransfers(
            actor(principal),
            tenantId,
            sourceTenantId,
            page,
            size
        );
        List<CompletedTransferResponse> content = result.content().stream()
            .map(v -> new CompletedTransferResponse(
                v.transferId(),
                v.passportId(),
                v.sourceTenantId(),
                v.serialNumber(),
                v.modelName(),
                v.assetState(),
                v.toOwnerId(),
                v.acceptMethod(),
                v.completedAt()
            ))
            .toList();
        return ApiResponse.success(new PagedCompletedTransferResponse(content, result.page(), result.size(), result.totalElements(),
            result.totalPages()));
    }

    @PostMapping("/transfers/{transferId}/accept")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('SCOPE_OWNER_TRANSFER_ACCEPT')")
    public ApiResponse<AcceptTransferResponse> accept(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("transferId") String transferId,
        @RequestBody AcceptTransferRequest request
    ) {
        AcceptTransferResult result = transferAcceptUseCase.accept(
            actor(principal),
            transferId,
            new AcceptTransferCommand(request.qrNonce(), request.password())
        );
        return ApiResponse.success(AcceptTransferResponse.from(result));
    }

    @PostMapping("/transfers/{transferId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SCOPE_OWNER_TRANSFER_CREATE', 'SCOPE_RETAIL_TRANSFER_CREATE')")
    public ApiResponse<CancelTransferResponse> cancel(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("transferId") String transferId
    ) {
        CancelTransferResult result = transferCancelUseCase.cancel(actor(principal), transferId);
        return ApiResponse.success(CancelTransferResponse.from(result));
    }

    private WorkflowActorContext actor(AuthPrincipal principal) {
        return WorkflowActorContext.from(principal);
    }

}
