package io.attestry.workflow.infrastructure.persistence.jpa.transfer.mapper;

import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.infrastructure.persistence.jpa.transfer.entity.WorkflowTokenTransferJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TokenTransferMapper {

    public TokenTransfer toDomain(WorkflowTokenTransferJpaEntity entity) {
        return new TokenTransfer(
            entity.getTransferId(),
            entity.getPassportId(),
            entity.getTransferType(),
            entity.getStatus(),
            entity.getAcceptMethod(),
            entity.getFromOwnerId(),
            entity.getToOwnerId(),
            entity.getTenantId(),
            entity.getQrNonce(),
            entity.getCodeHash(),
            entity.getCodeSalt(),
            entity.getAttemptCount(),
            entity.getExpiresAt(),
            entity.getCreatedAt(),
            entity.getCreatedByUserId(),
            entity.getCompletedAt(),
            entity.getCancelledAt(),
            entity.getCancelledByUserId()
        );
    }

    public WorkflowTokenTransferJpaEntity toEntity(TokenTransfer domain) {
        return new WorkflowTokenTransferJpaEntity(
            domain.transferId(),
            domain.passportId(),
            domain.transferType(),
            domain.status(),
            domain.acceptMethod(),
            domain.fromOwnerId(),
            domain.toOwnerId(),
            domain.tenantId(),
            domain.qrNonce(),
            domain.codeHash(),
            domain.codeSalt(),
            domain.attemptCount(),
            domain.expiresAt(),
            domain.createdAt(),
            domain.createdByUserId(),
            domain.completedAt(),
            domain.cancelledAt(),
            domain.cancelledByUserId()
        );
    }
}
