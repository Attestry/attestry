package io.attestry.workflow.application.delegation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.workflow.application.delegation.result.DelegationEvaluateResult;
import io.attestry.workflow.application.delegation.support.DelegationLifecycleService;
import io.attestry.workflow.application.port.delegation.DelegationPermissionProjectionPort;
import io.attestry.workflow.domain.delegation.model.Delegation;
import io.attestry.workflow.domain.delegation.repository.DelegationRepository;
import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.model.PartnerType;
import io.attestry.workflow.domain.partner.repository.PartnerLinkRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DelegationLifecycleServiceTest {

    @Mock DelegationRepository delegationRepository;
    @Mock PartnerLinkRepository partnerLinkRepository;
    @Mock DelegationPermissionProjectionPort permissionProjectionPort;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-12T00:00:00Z"), ZoneOffset.UTC);

    private DelegationLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new DelegationLifecycleService(
            delegationRepository,
            partnerLinkRepository,
            permissionProjectionPort,
            clock
        );
    }

    @Test
    void evaluate_expiresActiveDelegationAndSyncsProjection() {
        Delegation active = Delegation.grant(
            "pl-1",
            "source-tenant",
            "target-tenant",
            "PASSPORT",
            "passport-1",
            "RETAIL_TRANSFER_CREATE",
            Instant.parse("2026-03-01T00:00:00Z"),
            "user-1",
            Instant.parse("2026-02-01T00:00:00Z"),
            "note"
        );
        when(delegationRepository.findActive("source-tenant", "target-tenant", "PASSPORT", "passport-1", "RETAIL_TRANSFER_CREATE"))
            .thenReturn(Optional.of(active));
        when(delegationRepository.save(any(Delegation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DelegationEvaluateResult result = service.evaluate(
            "source-tenant",
            "target-tenant",
            "PASSPORT",
            "passport-1",
            "RETAIL_TRANSFER_CREATE"
        );

        assertEquals(false, result.allowed());
        assertEquals("DELEGATION_EXPIRED", result.reason());
        verify(delegationRepository).save(any(Delegation.class));
        verify(permissionProjectionPort).onDelegationExpired(any(Delegation.class));
        verify(partnerLinkRepository, never()).findById(any());
    }

    @Test
    void evaluate_deniesWhenPartnerLinkNotActive() {
        Delegation active = Delegation.grant(
            "pl-1",
            "source-tenant",
            "target-tenant",
            "PASSPORT",
            "passport-1",
            "RETAIL_TRANSFER_CREATE",
            Instant.parse("2026-04-01T00:00:00Z"),
            "user-1",
            Instant.parse("2026-03-01T00:00:00Z"),
            "note"
        );
        PartnerLink inactiveLink = new PartnerLink(
            "pl-1",
            "source-tenant",
            "target-tenant",
            PartnerType.RETAIL,
            PartnerLinkStatus.SUSPENDED,
            "user-1",
            Instant.parse("2026-03-01T00:00:00Z"),
            "approver-1",
            Instant.parse("2026-03-02T00:00:00Z"),
            null,
            null,
            null
        );
        when(delegationRepository.findActive("source-tenant", "target-tenant", "PASSPORT", "passport-1", "RETAIL_TRANSFER_CREATE"))
            .thenReturn(Optional.of(active));
        when(partnerLinkRepository.findById("pl-1")).thenReturn(Optional.of(inactiveLink));

        DelegationEvaluateResult result = service.evaluate(
            "source-tenant",
            "target-tenant",
            "PASSPORT",
            "passport-1",
            "RETAIL_TRANSFER_CREATE"
        );

        assertEquals(false, result.allowed());
        assertEquals("PARTNER_LINK_NOT_ACTIVE", result.reason());
        verify(delegationRepository, never()).save(any());
        verify(permissionProjectionPort, never()).onDelegationExpired(any());
    }

    @Test
    void consumeByPassportId_consumesActiveDelegationsAndSyncsProjection() {
        Delegation active = Delegation.grant(
            "pl-1",
            "source-tenant",
            "target-tenant",
            "PASSPORT",
            "passport-1",
            "RETAIL_TRANSFER_CREATE",
            Instant.parse("2026-04-01T00:00:00Z"),
            "user-1",
            Instant.parse("2026-03-01T00:00:00Z"),
            "note"
        );
        when(delegationRepository.findActiveByResourceId("PASSPORT", "passport-1")).thenReturn(List.of(active));
        when(delegationRepository.save(any(Delegation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.consumeByPassportId("passport-1");

        verify(delegationRepository).save(any(Delegation.class));
        verify(permissionProjectionPort).onDelegationConsumed(any(Delegation.class));
    }
}
