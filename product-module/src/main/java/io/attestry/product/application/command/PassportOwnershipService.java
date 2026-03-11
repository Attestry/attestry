package io.attestry.product.application.command;

import io.attestry.product.application.port.ownership.OwnershipUpdatePort;
import io.attestry.product.application.port.ownership.PassportOwnershipPort;
import io.attestry.product.domain.ownership.model.PassportOwnership;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PassportOwnershipService implements OwnershipUpdatePort {

    private final PassportOwnershipPort ownershipPort;
    private final Clock clock;

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
