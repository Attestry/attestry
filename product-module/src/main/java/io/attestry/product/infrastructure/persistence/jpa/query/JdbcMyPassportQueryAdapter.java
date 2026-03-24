package io.attestry.product.infrastructure.persistence.jpa.query;

import io.attestry.product.application.query.view.MyPassportView;
import io.attestry.product.application.port.query.MyPassportQueryPort;
import io.attestry.product.infrastructure.persistence.jpa.repository.PassportOwnershipJpaRepository;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcMyPassportQueryAdapter implements MyPassportQueryPort {

    private final PassportOwnershipJpaRepository passportOwnershipRepository;

    @Override
    public List<MyPassportView> findByOwnerId(String ownerId) {
        return passportOwnershipRepository.findMyPassportsByOwnerId(ownerId).stream()
            .map(p -> new MyPassportView(
                p.getPassportId(),
                p.getQrPublicCode(),
                p.getTenantId(),
                p.getAssetId(),
                p.getSerialNumber(),
                p.getModelName(),
                p.getAssetState(),
                p.getRiskFlag(),
                p.getOwnedSince() == null ? null : p.getOwnedSince().atZone(ZoneOffset.UTC).toInstant()
            ))
            .toList();
    }
}
