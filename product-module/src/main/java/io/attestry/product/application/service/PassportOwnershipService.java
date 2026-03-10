package io.attestry.product.application.service;

import io.attestry.product.application.port.OwnershipUpdatePort;
import io.attestry.product.application.port.PassportOwnershipPort;
import io.attestry.product.domain.ownership.model.PassportOwnership;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PassportOwnershipService implements OwnershipUpdatePort {

    private final PassportOwnershipPort ownershipPort;
    private final Clock clock;

    public PassportOwnershipService(PassportOwnershipPort ownershipPort, Clock clock) {
        this.ownershipPort = ownershipPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void updateOwner(String passportId, String newOwnerId) {
        Instant now = Instant.now(clock);
        PassportOwnership ownership = ownershipPort.findByPassportId(passportId)
            .orElseGet(() -> PassportOwnership.empty(passportId));
        ownership.updateOwner(newOwnerId, 0, now);
        ownershipPort.save(ownership);
    }
}
