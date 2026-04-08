package io.attestry.workflow.application.transfer.internal;

import io.attestry.workflow.application.transfer.command.CreateB2CTransferCommand;
import io.attestry.workflow.application.transfer.command.CreateC2CTransferCommand;
import io.attestry.workflow.application.transfer.result.CreateTransferResult;
import io.attestry.workflow.domain.transfer.model.AcceptCredential;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.policy.TransferCreatePolicy;
import io.attestry.workflow.domain.transfer.policy.TransferCreatePolicy.TransferCreateContext;
import io.attestry.workflow.domain.transfer.repository.TokenTransferRepository;
import io.attestry.workflow.domain.transfer.service.AcceptCredentialFactory;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferCreateExecutor {

    private final TokenTransferRepository transferRepository;
    private final AcceptCredentialFactory credentialFactory;
    private final TransferCreatePolicy createPolicy;
    private final TransferCreateResultMapper resultMapper;
    private final Clock clock;

    public CreateTransferResult createC2C(String passportId, String actorUserId, CreateC2CTransferCommand command, TransferCreateContext context) {
        createPolicy.assertC2CCreatable(context);

        Instant now = Instant.now(clock);
        AcceptCredential credential = credentialFactory.create(command.acceptMethod(), command.password());

        TokenTransfer transfer = TokenTransfer.createC2C(
            UUID.randomUUID().toString(),
            passportId,
            context.currentOwnerId(),
            credential,
            command.expiresAt(),
            now,
            actorUserId
        );
        return resultMapper.toResult(transferRepository.save(transfer));
    }

    public CreateTransferResult createB2C(
        String passportId,
        String tenantId,
        String actorUserId,
        CreateB2CTransferCommand command,
        TransferCreateContext context
    ) {
        createPolicy.assertB2CCreatable(context);

        Instant now = Instant.now(clock);
        AcceptCredential credential = credentialFactory.create(command.acceptMethod(), command.password());

        TokenTransfer transfer = TokenTransfer.createB2C(
            UUID.randomUUID().toString(),
            passportId,
            tenantId,
            credential,
            command.expiresAt(),
            now,
            actorUserId
        );
        return resultMapper.toResult(transferRepository.save(transfer));
    }
}
