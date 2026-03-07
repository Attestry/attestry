package io.attestry.workflow.application.partner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.identity.model.VerificationLevel;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.TenantReadPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.partner.repository.PartnerLinkRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartnerLinkServiceTenantSearchTest {

    @Mock PartnerLinkRepository repository;
    @Mock TenantReadPort tenantReadPort;
    @Mock WorkflowAuthorizationSupport authorizationSupport;

    private PartnerLinkService service;

    private static final String TENANT_ID = "tenant-1";
    private static final AuthPrincipal PRINCIPAL = new AuthPrincipal(
        "token-1",
        "user-1",
        TENANT_ID,
        VerificationLevel.PHONE_VERIFIED,
        Set.of("SCOPE_PARTNER_LINK_READ"),
        Instant.parse("2026-03-10T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);
        service = new PartnerLinkService(repository, tenantReadPort, authorizationSupport, clock);
    }

    @Test
    void searchActiveTenantsByName_returnsNameRegionTypeList() {
        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(tenantReadPort.searchActiveTenantsByName("brand"))
            .thenReturn(List.of(
                new TenantReadPort.TenantSummary("tenant-kr", "Brand Korea", "KR", "BRAND"),
                new TenantReadPort.TenantSummary("tenant-us", "Brand US", "US", "BRAND")
            ));

        var result = service.searchActiveTenantsByName(PRINCIPAL, " brand ");

        assertEquals(2, result.size());
        assertEquals("tenant-kr", result.get(0).tenantId());
        assertEquals("Brand Korea", result.get(0).name());
        assertEquals("KR", result.get(0).region());
        assertEquals("BRAND", result.get(0).type());
        verify(tenantReadPort).searchActiveTenantsByName("brand");
        verify(authorizationSupport).assertLivePermission(
            eq(PRINCIPAL), eq(TENANT_ID), eq(PermissionCodes.PARTNER_LINK_READ), eq("partner-link:tenant-search")
        );
    }

    @Test
    void searchActiveTenantsByName_blankName_throwsInvalidRequest() {
        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());

        WorkflowDomainException ex = assertThrows(
            WorkflowDomainException.class,
            () -> service.searchActiveTenantsByName(PRINCIPAL, " ")
        );

        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
        assertEquals("name is required", ex.getMessage());
    }
}
