package io.attestry.product.infrastructure.persistence.jpa.projection;

import io.attestry.product.application.dto.view.DistributedPassportDetailView;
import io.attestry.product.application.dto.view.DistributedPassportView;
import io.attestry.product.application.port.query.DistributedPassportQueryPort;
import io.attestry.product.application.port.projection.ProductRetailAccessProjectionPort;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.infrastructure.persistence.jpa.repository.RetailAccessProjectionJpaRepository;
import io.attestry.product.infrastructure.persistence.jpa.repository.RetailAccessProjectionJpaRepository.RetailAccessDetailProjection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcProductRetailAccessProjectionAdapter
    implements ProductRetailAccessProjectionPort, DistributedPassportQueryPort {

    private final RetailAccessProjectionJpaRepository retailAccessRepository;

    @Override
    public PagedRetailAccessResult findAccessiblePassports(
        String tenantId,
        int page,
        int size,
        String keyword,
        String sourceTenantId
    ) {
        return retailAccessRepository.findAccessiblePassportsWithFilters(
            tenantId, page, size, keyword, sourceTenantId
        );
    }

    @Override
    public PagedResult findByTargetTenant(String tenantId, int page, int size, String keyword, String sourceTenantId) {
        PagedRetailAccessResult result = findAccessiblePassports(tenantId, page, size, keyword, sourceTenantId);
        List<DistributedPassportView> content = result.content().stream()
            .map(row -> new DistributedPassportView(
                row.passportId(),
                row.qrPublicCode(),
                row.assetId(),
                row.serialNumber(),
                row.modelId(),
                row.modelName(),
                row.assetState(),
                row.riskFlag(),
                row.accessSourceType().equals("PERMISSION") ? row.accessSourceId() : null,
                row.expiresAt(),
                row.sourceTenantId(),
                row.targetTenantId(),
                row.accessStatus(),
                row.grantedAt()
            ))
            .toList();
        return new PagedResult(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    @Override
    public Optional<RetailAccessDetailView> findAccessiblePassportDetail(String tenantId, String passportId) {
        List<RetailAccessDetailProjection> rows =
            retailAccessRepository.findAccessiblePassportDetailByTenantAndPassport(tenantId, passportId);

        return rows.stream().findFirst()
            .map(p -> new RetailAccessDetailView(
                p.getPassportId(),
                p.getQrPublicCode(),
                p.getSerialNumber(),
                p.getModelId(),
                p.getModelName(),
                p.getAssetState(),
                p.getRiskFlag(),
                toInstant(p.getManufacturedAt()),
                p.getProductionBatch(),
                p.getFactoryCode(),
                p.getAccessSourceType(),
                p.getAccessSourceId(),
                p.getUpdatedAt().toInstant()
            ));
    }

    @Override
    public DistributedPassportDetailView findDetailByRetailAccess(String tenantId, String passportId) {
        return findAccessiblePassportDetail(tenantId, passportId)
            .map(row -> new DistributedPassportDetailView(
                row.passportId(),
                row.qrPublicCode(),
                row.serialNumber(),
                row.modelId(),
                row.modelName(),
                row.assetState(),
                row.riskFlag(),
                row.manufacturedAt(),
                row.productionBatch(),
                row.factoryCode()
            ))
            .orElseThrow(() -> new ProductDomainException(
                ProductErrorCode.ASSET_NOT_FOUND,
                "Distributed passport not found for tenant: " + passportId
            ));
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
