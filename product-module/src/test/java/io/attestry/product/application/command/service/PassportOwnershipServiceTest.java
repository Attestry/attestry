package io.attestry.product.application.command.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.product.application.port.ownership.PassportOwnershipPort;
import io.attestry.product.domain.ownership.model.PassportOwnership;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PassportOwnershipServiceTest {

    @Mock PassportOwnershipPort ownershipPort;

    private final Clock clock = Clock.fixed(Instant.parse("2026-04-01T00:00:00Z"), ZoneOffset.UTC);

    private PassportOwnershipService service;

    @BeforeEach
    void setUp() {
        service = new PassportOwnershipService(ownershipPort, clock);
    }

    @Test
    void updateOwner_existingOwnership_updatesOwner() {
        PassportOwnership existing = PassportOwnership.reconstitute(
            "p1", "old-owner", Instant.parse("2026-03-01T00:00:00Z")
        );
        when(ownershipPort.findByPassportId("p1")).thenReturn(Optional.of(existing));

        service.updateOwner("p1", "new-owner");

        ArgumentCaptor<PassportOwnership> captor = ArgumentCaptor.forClass(PassportOwnership.class);
        verify(ownershipPort).save(captor.capture());
        PassportOwnership saved = captor.getValue();
        assertEquals("new-owner", saved.getOwnerId());
        assertEquals(Instant.parse("2026-04-01T00:00:00Z"), saved.getUpdatedAt());
    }

    @Test
    void updateOwner_noExistingOwnership_createsNewAndUpdates() {
        when(ownershipPort.findByPassportId("p1")).thenReturn(Optional.empty());

        service.updateOwner("p1", "new-owner");

        ArgumentCaptor<PassportOwnership> captor = ArgumentCaptor.forClass(PassportOwnership.class);
        verify(ownershipPort).save(captor.capture());
        PassportOwnership saved = captor.getValue();
        assertEquals("p1", saved.getPassportId());
        assertEquals("new-owner", saved.getOwnerId());
    }
}
