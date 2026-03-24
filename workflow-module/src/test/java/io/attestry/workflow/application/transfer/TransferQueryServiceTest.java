package io.attestry.workflow.application.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.userauth.domain.auth.model.VerificationLevel;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.transfer.CompletedTransferQueryPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferQueryServiceTest {

    @Mock CompletedTransferQueryPort completedTransferQueryPort;
    @Mock WorkflowAuthorizationSupport authorizationSupport;

    private TransferQueryService service;

    private static final AuthPrincipal RETAIL_PRINCIPAL = new AuthPrincipal(
        "token1", "retail-user", "tenant-retail", VerificationLevel.PHONE_VERIFIED,
        Set.of("SCOPE_TENANT_READ_ONLY"), Instant.parse("2026-03-12T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new TransferQueryService(completedTransferQueryPort, authorizationSupport);
    }

    @Test
    void listCompletedB2CTransfers_returnsPagedRows() {
        doNothing().when(authorizationSupport).assertTenantContext(RETAIL_PRINCIPAL, "tenant-retail");
        doNothing().when(authorizationSupport).assertLivePermission(
            RETAIL_PRINCIPAL,
            "tenant-retail",
            "TENANT_READ_ONLY",
            "transfer:completed:tenant-retail"
        );
        when(completedTransferQueryPort.findCompletedB2CByTenantId("tenant-retail", "brand-tenant-1", 0, 20)).thenReturn(
            new CompletedTransferQueryPort.PagedResult(
                List.of(new CompletedTransferQueryPort.CompletedTransferRow(
                    "transfer-1",
                    "passport-1",
                    "brand-tenant-1",
                    "SN-001",
                    "Model Name",
                    "ACTIVE",
                    "owner-1",
                    "QR",
                    Instant.parse("2026-03-09T10:00:00Z")
                )),
                0,
                20,
                1,
                1
            )
        );

        var result = service.listCompletedB2CTransfers(RETAIL_PRINCIPAL, "tenant-retail", "brand-tenant-1", 0, 20);

        assertEquals(1, result.content().size());
        assertEquals("transfer-1", result.content().get(0).transferId());
        assertEquals("passport-1", result.content().get(0).passportId());
        assertEquals("brand-tenant-1", result.content().get(0).sourceTenantId());
        assertEquals("owner-1", result.content().get(0).toOwnerId());
        verify(completedTransferQueryPort).findCompletedB2CByTenantId("tenant-retail", "brand-tenant-1", 0, 20);
    }
}
