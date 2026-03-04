package io.attestry.product.application.service;

import io.attestry.product.application.port.OwnershipUpdatePort;
import io.attestry.product.domain.ownership.model.PassportOwnership;
import io.attestry.product.domain.ownership.repository.PassportOwnershipRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PassportOwnershipService implements OwnershipUpdatePort {

    private final PassportOwnershipRepository ownershipRepository;
    private final Clock clock;

    public PassportOwnershipService(PassportOwnershipRepository ownershipRepository, Clock clock) {
        this.ownershipRepository = ownershipRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void updateOwner(String passportId, String newOwnerId) {
        Instant now = Instant.now(clock);
        PassportOwnership ownership = ownershipRepository.findByPassportId(passportId)
            .orElseGet(() -> PassportOwnership.empty(passportId));
        ownership.updateOwner(newOwnerId, 0, now);
        ownershipRepository.save(ownership);
    }
}
