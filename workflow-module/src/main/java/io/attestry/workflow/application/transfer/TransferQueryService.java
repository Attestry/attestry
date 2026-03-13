package io.attestry.workflow.application.transfer;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.transfer.CompletedTransferQueryPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.TransferQueryUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransferQueryService implements TransferQueryUseCase {

    private final CompletedTransferQueryPort completedTransferQueryPort;
    private final WorkflowAuthorizationSupport authorizationSupport;

    public TransferQueryService(
        CompletedTransferQueryPort completedTransferQueryPort,
        WorkflowAuthorizationSupport authorizationSupport
    ) {
        this.completedTransferQueryPort = completedTransferQueryPort;
        this.authorizationSupport = authorizationSupport;
    }

    @Override
    public PagedCompletedTransferResponse listCompletedB2CTransfers(
        AuthPrincipal principal,
        String tenantId,
        String sourceTenantId,
        int page,
        int size
    ) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.TENANT_READ_ONLY,
            "transfer:completed:" + tenantId);

        CompletedTransferQueryPort.PagedResult paged = completedTransferQueryPort.findCompletedB2CByTenantId(
            tenantId,
            sourceTenantId,
            page,
            size
        );

        return new PagedCompletedTransferResponse(
            paged.content().stream()
                .map(row -> new CompletedTransferView(
                    row.transferId(),
                    row.passportId(),
                    row.sourceTenantId(),
                    row.serialNumber(),
                    row.modelName(),
                    row.assetState(),
                    row.toOwnerId(),
                    row.acceptMethod(),
                    row.completedAt()
                ))
                .toList(),
            paged.page(),
            paged.size(),
            paged.totalElements(),
            paged.totalPages()
        );
    }

}
