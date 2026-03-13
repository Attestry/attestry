package io.attestry.workflow.application.partner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.identity.model.VerificationLevel;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.partner.command.CreatePartnerLinkCommand;
import io.attestry.workflow.application.partner.result.PartnerLinkResult;
import io.attestry.workflow.application.port.common.TenantReadPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.model.PartnerType;
import io.attestry.workflow.domain.partner.policy.PartnerLinkCreatePolicy;
import io.attestry.workflow.domain.partner.repository.PartnerLinkRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartnerLinkServiceTest {

    @Mock PartnerLinkRepository repository;
    @Mock TenantReadPort tenantReadPort;
    @Mock WorkflowAuthorizationSupport authorizationSupport;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-12T01:00:00Z"), ZoneOffset.UTC);
    private final PartnerLinkCreatePolicy createPolicy = new PartnerLinkCreatePolicy();

    private PartnerLinkService service;

    private static final AuthPrincipal SOURCE_PRINCIPAL = new AuthPrincipal(
        "token-1",
        "user-1",
        "tenant-source",
        VerificationLevel.PHONE_VERIFIED,
        Set.of("SCOPE_PARTNER_LINK_CREATE", "SCOPE_PARTNER_LINK_APPROVE", "SCOPE_TENANT_READ_ONLY"),
        Instant.parse("2026-03-13T00:00:00Z")
    );
    private static final AuthPrincipal TARGET_PRINCIPAL = new AuthPrincipal(
        "token-3",
        "user-3",
        "tenant-target",
        VerificationLevel.PHONE_VERIFIED,
        Set.of("SCOPE_PARTNER_LINK_APPROVE", "SCOPE_TENANT_READ_ONLY"),
        Instant.parse("2026-03-13T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new PartnerLinkService(repository, tenantReadPort, authorizationSupport, createPolicy, clock);
    }

    @Test
    void create_savesPendingPartnerLink() {
        CreatePartnerLinkCommand command = new CreatePartnerLinkCommand(
            "tenant-target",
            PartnerType.RETAIL,
            Instant.parse("2026-04-01T00:00:00Z"),
            "message"
        );
        PartnerLink saved = new PartnerLink(
            "pl-1",
            "tenant-source",
            "tenant-target",
            PartnerType.RETAIL,
            PartnerLinkStatus.PENDING,
            "user-1",
            Instant.parse("2026-03-12T01:00:00Z"),
            null,
            null,
            Instant.parse("2026-04-01T00:00:00Z"),
            null,
            null
        );

        doNothing().when(authorizationSupport).assertTenantContext(SOURCE_PRINCIPAL, "tenant-source");
        doNothing().when(authorizationSupport).assertLivePermission(
            SOURCE_PRINCIPAL, "tenant-source", PermissionCodes.PARTNER_LINK_CREATE, "partner-link:create"
        );
        when(tenantReadPort.existsActiveTenant("tenant-source")).thenReturn(true);
        when(tenantReadPort.existsActiveTenant("tenant-target")).thenReturn(true);
        when(repository.existsBySourceAndTargetAndTypeAndStatus(
            "tenant-source", "tenant-target", PartnerType.RETAIL, PartnerLinkStatus.ACTIVE
        )).thenReturn(false);
        when(repository.save(any(PartnerLink.class))).thenReturn(saved);
        when(tenantReadPort.findTenantSummary("tenant-source"))
            .thenReturn(new TenantReadPort.TenantSummary("tenant-source", "Source Tenant", "KR", "addr", "BRAND"));
        when(tenantReadPort.findTenantNamesByIds(List.of("tenant-target")))
            .thenReturn(Map.of("tenant-target", "Target Tenant"));

        PartnerLinkResult result = service.create(SOURCE_PRINCIPAL, command);

        assertEquals("pl-1", result.partnerLinkId());
        assertEquals("tenant-source", result.sourceTenantId());
        assertEquals("Source Tenant", result.sourceTenantName());
        assertEquals("tenant-target", result.targetTenantId());
        assertEquals("Target Tenant", result.targetTenantName());
        assertEquals("PENDING", result.status());
        verify(repository).save(any(PartnerLink.class));
    }

    @Test
    void create_sameTenant_throws() {
        CreatePartnerLinkCommand command = new CreatePartnerLinkCommand(
            "tenant-source",
            PartnerType.RETAIL,
            null,
            null
        );

        doNothing().when(authorizationSupport).assertTenantContext(SOURCE_PRINCIPAL, "tenant-source");
        doNothing().when(authorizationSupport).assertLivePermission(
            SOURCE_PRINCIPAL, "tenant-source", PermissionCodes.PARTNER_LINK_CREATE, "partner-link:create"
        );
        when(tenantReadPort.existsActiveTenant("tenant-source")).thenReturn(true);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () -> service.create(SOURCE_PRINCIPAL, command));

        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
        verify(repository, never()).save(any());
    }

    @Test
    void create_inactiveTenant_throws() {
        CreatePartnerLinkCommand command = new CreatePartnerLinkCommand(
            "tenant-target",
            PartnerType.RETAIL,
            null,
            null
        );

        doNothing().when(authorizationSupport).assertTenantContext(SOURCE_PRINCIPAL, "tenant-source");
        doNothing().when(authorizationSupport).assertLivePermission(
            SOURCE_PRINCIPAL, "tenant-source", PermissionCodes.PARTNER_LINK_CREATE, "partner-link:create"
        );
        when(tenantReadPort.existsActiveTenant("tenant-source")).thenReturn(true);
        when(tenantReadPort.existsActiveTenant("tenant-target")).thenReturn(false);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () -> service.create(SOURCE_PRINCIPAL, command));

        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
        verify(repository, never()).save(any());
    }

    @Test
    void create_duplicateActiveLink_throws() {
        CreatePartnerLinkCommand command = new CreatePartnerLinkCommand(
            "tenant-target",
            PartnerType.RETAIL,
            null,
            null
        );

        doNothing().when(authorizationSupport).assertTenantContext(SOURCE_PRINCIPAL, "tenant-source");
        doNothing().when(authorizationSupport).assertLivePermission(
            SOURCE_PRINCIPAL, "tenant-source", PermissionCodes.PARTNER_LINK_CREATE, "partner-link:create"
        );
        when(tenantReadPort.existsActiveTenant("tenant-source")).thenReturn(true);
        when(tenantReadPort.existsActiveTenant("tenant-target")).thenReturn(true);
        when(repository.existsBySourceAndTargetAndTypeAndStatus(
            "tenant-source", "tenant-target", PartnerType.RETAIL, PartnerLinkStatus.ACTIVE
        )).thenReturn(true);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () -> service.create(SOURCE_PRINCIPAL, command));

        assertEquals(WorkflowErrorCode.PARTNER_LINK_ALREADY_ACTIVE, ex.getErrorCode());
        verify(repository, never()).save(any());
    }

    @Test
    void create_pastExpiry_throws() {
        CreatePartnerLinkCommand command = new CreatePartnerLinkCommand(
            "tenant-target",
            PartnerType.RETAIL,
            Instant.parse("2026-03-01T00:00:00Z"),
            null
        );

        doNothing().when(authorizationSupport).assertTenantContext(SOURCE_PRINCIPAL, "tenant-source");
        doNothing().when(authorizationSupport).assertLivePermission(
            SOURCE_PRINCIPAL, "tenant-source", PermissionCodes.PARTNER_LINK_CREATE, "partner-link:create"
        );
        when(tenantReadPort.existsActiveTenant("tenant-source")).thenReturn(true);
        when(tenantReadPort.existsActiveTenant("tenant-target")).thenReturn(true);
        when(repository.existsBySourceAndTargetAndTypeAndStatus(
            "tenant-source", "tenant-target", PartnerType.RETAIL, PartnerLinkStatus.ACTIVE
        )).thenReturn(false);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () -> service.create(SOURCE_PRINCIPAL, command));

        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
        verify(repository, never()).save(any());
    }

    @Test
    void approve_savesActivatedPartnerLink() {
        PartnerLink pending = new PartnerLink(
            "pl-1",
            "tenant-source",
            "tenant-target",
            PartnerType.RETAIL,
            PartnerLinkStatus.PENDING,
            "user-2",
            Instant.parse("2026-03-10T00:00:00Z"),
            null,
            null,
            null,
            null,
            null
        );
        PartnerLink approved = pending.approve("user-1", Instant.parse("2026-03-12T01:00:00Z"));

        when(repository.findById("pl-1")).thenReturn(Optional.of(pending));
        doNothing().when(authorizationSupport).assertTenantContext(TARGET_PRINCIPAL, "tenant-target");
        doNothing().when(authorizationSupport).assertLivePermission(
            TARGET_PRINCIPAL, "tenant-target", PermissionCodes.PARTNER_LINK_APPROVE, "partner-link:pl-1"
        );
        when(repository.save(any(PartnerLink.class))).thenReturn(approved);
        when(tenantReadPort.findTenantSummary("tenant-source"))
            .thenReturn(new TenantReadPort.TenantSummary("tenant-source", "Source Tenant", "KR", "addr", "BRAND"));
        when(tenantReadPort.findTenantNamesByIds(List.of("tenant-target")))
            .thenReturn(Map.of("tenant-target", "Target Tenant"));

        PartnerLinkResult result = service.approve(TARGET_PRINCIPAL, "pl-1");

        assertEquals("ACTIVE", result.status());
        verify(repository).findById("pl-1");
        verify(repository).save(any(PartnerLink.class));
        verify(authorizationSupport).assertTenantContext(TARGET_PRINCIPAL, "tenant-target");
    }

    @Test
    void listByTenant_withStatus_filtersResults() {
        PartnerLink active = new PartnerLink(
            "pl-1",
            "tenant-source",
            "tenant-target",
            PartnerType.RETAIL,
            PartnerLinkStatus.ACTIVE,
            "user-1",
            Instant.parse("2026-03-10T00:00:00Z"),
            "approver-1",
            Instant.parse("2026-03-11T00:00:00Z"),
            null,
            null,
            null
        );

        doNothing().when(authorizationSupport).assertTenantContext(SOURCE_PRINCIPAL, "tenant-source");
        when(repository.findByTenantId("tenant-source", PartnerLinkStatus.ACTIVE)).thenReturn(List.of(active));
        when(tenantReadPort.findTenantSummariesByIds(List.of("tenant-source")))
            .thenReturn(Map.of(
                "tenant-source",
                new TenantReadPort.TenantSummary("tenant-source", "Source Tenant", "KR", "addr", "BRAND")
            ));
        when(tenantReadPort.findTenantNamesByIds(List.of("tenant-target")))
            .thenReturn(Map.of("tenant-target", "Target Tenant"));

        List<PartnerLinkResult> results = service.listByTenant(SOURCE_PRINCIPAL, PartnerLinkStatus.ACTIVE);

        assertEquals(1, results.size());
        assertEquals("ACTIVE", results.get(0).status());
        assertEquals("Source Tenant", results.get(0).sourceTenantName());
        verify(repository).findByTenantId("tenant-source", PartnerLinkStatus.ACTIVE);
        verify(tenantReadPort).findTenantSummariesByIds(List.of("tenant-source"));
        verify(tenantReadPort).findTenantNamesByIds(List.of("tenant-target"));
    }

    @Test
    void approve_checksTargetTenantContext() {
        AuthPrincipal foreignPrincipal = new AuthPrincipal(
            "token-2",
            "user-9",
            "tenant-foreign",
            VerificationLevel.PHONE_VERIFIED,
            Set.of("SCOPE_PARTNER_LINK_APPROVE"),
            Instant.parse("2026-03-13T00:00:00Z")
        );
        PartnerLink pending = new PartnerLink(
            "pl-1",
            "tenant-source",
            "tenant-target",
            PartnerType.RETAIL,
            PartnerLinkStatus.PENDING,
            "user-2",
            Instant.parse("2026-03-10T00:00:00Z"),
            null,
            null,
            null,
            null,
            null
        );

        when(repository.findById("pl-1")).thenReturn(Optional.of(pending));
        doNothing().when(authorizationSupport).assertTenantContext(foreignPrincipal, "tenant-target");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tenantReadPort.findTenantSummary("tenant-source"))
            .thenReturn(new TenantReadPort.TenantSummary("tenant-source", "Source Tenant", "KR", "addr", "BRAND"));
        when(tenantReadPort.findTenantNamesByIds(List.of("tenant-target")))
            .thenReturn(Map.of("tenant-target", "Target Tenant"));

        service.approve(foreignPrincipal, "pl-1");

        verify(authorizationSupport).assertTenantContext(foreignPrincipal, "tenant-target");
        verify(authorizationSupport).assertLivePermission(
            foreignPrincipal, "tenant-target", PermissionCodes.PARTNER_LINK_APPROVE, "partner-link:pl-1"
        );
    }
}
