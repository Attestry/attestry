package io.attestry.workflow.application.transfer;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.TransferProductReadPort;
import io.attestry.workflow.application.port.TransferProductReadPort.TransferPassportState;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.transfer.command.CreateB2CTransferCommand;
import io.attestry.workflow.application.transfer.command.CreateC2CTransferCommand;
import io.attestry.workflow.application.transfer.result.CreateTransferResult;
import io.attestry.workflow.application.usecase.TransferCreateUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.transfer.model.AcceptCredential;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.policy.TransferCreatePolicy;
import io.attestry.workflow.domain.transfer.policy.TransferCreatePolicy.TransferCreateContext;
import io.attestry.workflow.domain.transfer.repository.TokenTransferRepository;
import io.attestry.workflow.domain.transfer.service.AcceptCredentialFactory;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferCreateService implements TransferCreateUseCase {

    private final TokenTransferRepository transferRepository;
    private final TransferProductReadPort productReadPort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final AcceptCredentialFactory credentialFactory;
    private final TransferCreatePolicy createPolicy;
    private final Clock clock;

    public TransferCreateService(
        TokenTransferRepository transferRepository,
        TransferProductReadPort productReadPort,
        WorkflowAuthorizationSupport authorizationSupport,
        AcceptCredentialFactory credentialFactory,
        TransferCreatePolicy createPolicy,
        Clock clock
    ) {
        this.transferRepository = transferRepository;
        this.productReadPort = productReadPort;
        this.authorizationSupport = authorizationSupport;
        this.credentialFactory = credentialFactory;
        this.createPolicy = createPolicy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreateTransferResult createC2C(
        AuthPrincipal principal,
        String passportId,
        CreateC2CTransferCommand command
    ) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_TRANSFER_CREATE, "transfer:create:" + passportId);

        TransferCreateContext context = resolveContext(principal.userId(), null, passportId);
        createPolicy.assertC2CCreatable(context);

        Instant now = Instant.now(clock);
        AcceptCredential credential = credentialFactory.create(command.acceptMethod(), command.password());

        TokenTransfer transfer = TokenTransfer.createC2C(
            UUID.randomUUID().toString(), passportId, context.currentOwnerId(),
            credential, command.expiresAt(), now, principal.userId()
        );
        TokenTransfer saved = transferRepository.save(transfer);
        return toResult(saved);
    }

    @Override
    @Transactional
    public CreateTransferResult createB2C(
        AuthPrincipal principal,
        String tenantId,
        String passportId,
        CreateB2CTransferCommand command
    ) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.RETAIL_TRANSFER_CREATE, "transfer:create:" + passportId);

        TransferCreateContext context = resolveContext(principal.userId(), tenantId, passportId);
        createPolicy.assertB2CCreatable(context);

        Instant now = Instant.now(clock);
        AcceptCredential credential = credentialFactory.create(command.acceptMethod(), command.password());

        TokenTransfer transfer = TokenTransfer.createB2C(
            UUID.randomUUID().toString(), passportId, tenantId,
            credential, command.expiresAt(), now, principal.userId()
        );
        TokenTransfer saved = transferRepository.save(transfer);
        return toResult(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CreateTransferResult> findLatestActivePendingByPassportId(
        AuthPrincipal principal,
        String passportId
    ) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_TRANSFER_CREATE, "transfer:pending:" + passportId);
        String currentOwnerId = productReadPort.findCurrentOwnerId(passportId).orElse(null);
        if (!principal.userId().equals(currentOwnerId)) {
            throw new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Only current owner can view pending C2C transfer");
        }

        Instant now = Instant.now(clock);
        return transferRepository.findLatestActivePendingByPassportId(passportId, now)
            .map(this::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CreateTransferResult> findLatestActivePendingB2CByPassportId(
        AuthPrincipal principal,
        String tenantId,
        String passportId
    ) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.RETAIL_TRANSFER_CREATE,
            "transfer:pending:" + passportId);
        if (!productReadPort.hasRetailPermission(passportId, tenantId)) {
            throw new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE,
                "Retail tenant does not have permission for this passport");
        }

        Instant now = Instant.now(clock);
        return transferRepository.findLatestActivePendingByPassportId(passportId, now)
            .filter(transfer -> transfer.transferType() == io.attestry.workflow.domain.transfer.model.TransferType.B2C)
            .filter(transfer -> tenantId.equals(transfer.tenantId()))
            .map(this::toResult);
    }

    private TransferCreateContext resolveContext(String actorUserId, String requestTenantId, String passportId) {
        TransferPassportState state = productReadPort.findPassportState(passportId).orElse(null);
        String currentOwnerId = productReadPort.findCurrentOwnerId(passportId).orElse(null);
        boolean pendingExists = transferRepository.existsActivePendingByPassportId(passportId);
        boolean hasRetail = requestTenantId != null
            && productReadPort.hasRetailPermission(passportId, requestTenantId);

        return new TransferCreateContext(
            actorUserId,
            requestTenantId,
            state == null ? null : state.assetState(),
            state == null ? null : state.riskFlag(),
            state == null ? null : state.tenantId(),
            currentOwnerId,
            hasRetail,
            pendingExists
        );
    }

    private CreateTransferResult toResult(TokenTransfer saved) {
        return new CreateTransferResult(
            saved.transferId(), saved.passportId(),
            saved.transferType().name(), saved.status().name(),
            saved.acceptMethod().name(), saved.qrNonce(), saved.expiresAt()
        );
    }
}
