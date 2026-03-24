package io.attestry.product.application.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.product.application.port.query.DistributedPassportQueryPort;
import io.attestry.product.application.port.query.GroupPassportQueryPort;
import io.attestry.product.application.port.query.MyPassportQueryPort;
import io.attestry.product.application.port.query.PassportDistributionQueryPort;
import io.attestry.product.application.port.ownership.PassportOwnershipPort;
import io.attestry.product.application.port.permission.PassportPermissionPort;
import io.attestry.product.application.port.passport.PassportPort;
import io.attestry.product.application.port.query.PassportShipmentQueryPort;
import io.attestry.product.application.query.assembler.ProductQueryViewAssembler;
import io.attestry.product.application.query.view.DistributedPassportView;
import io.attestry.product.application.query.view.DistributedPassportDetailView;
import io.attestry.product.application.query.view.PagedDistributedPassportView;
import io.attestry.product.infrastructure.config.ProductProperties;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceTest {

    @Mock PassportPort passportPort;
    @Mock PassportOwnershipPort ownershipPort;
    @Mock PassportPermissionPort permissionPort;
    @Mock MyPassportQueryPort myPassportQueryPort;
    @Mock GroupPassportQueryPort groupPassportQueryPort;
    @Mock DistributedPassportQueryPort distributedPassportQueryPort;
    @Mock PassportShipmentQueryPort shipmentQueryPort;
    @Mock PassportDistributionQueryPort distributionQueryPort;
    @Mock ProductQueryViewAssembler viewAssembler;

    private ProductQueryService service;

    @BeforeEach
    void setUp() {
        service = new ProductQueryService(
            passportPort,
            ownershipPort,
            permissionPort,
            myPassportQueryPort,
            groupPassportQueryPort,
            distributedPassportQueryPort,
            shipmentQueryPort,
            distributionQueryPort,
            viewAssembler,
            productProperties()
        );
    }

    @Test
    void listDistributedPassports_returnsPagedPermissionBackedProducts() {
        List<DistributedPassportView> views = List.of(new DistributedPassportView(
            "passport-1",
            "QR-001",
            "asset-1",
            "SN-001",
            "MODEL-1",
            "Model Name",
            "ACTIVE",
            "NONE",
            "perm-1",
            Instant.parse("2026-04-01T00:00:00Z"),
            "brand-tenant",
            "retail-tenant",
            "ACTIVE",
            Instant.parse("2026-03-01T00:00:00Z")
        ));
        DistributedPassportQueryPort.PagedResult pagedResult = new DistributedPassportQueryPort.PagedResult(
            views, 0, 20, 1, 1
        );
        when(distributedPassportQueryPort.findByTargetTenant("retail-tenant", 0, 20, "SN", null))
            .thenReturn(pagedResult);
        when(viewAssembler.toPagedDistributedPassportView(pagedResult))
            .thenReturn(new PagedDistributedPassportView(views, 0, 20, 1, 1));

        var result = service.listDistributedPassports("retail-tenant", 0, 20, "  SN  ", null);

        assertEquals(1, result.content().size());
        assertEquals("passport-1", result.content().get(0).passportId());
        assertEquals("perm-1", result.content().get(0).permissionId());
        assertEquals("brand-tenant", result.content().get(0).sourceTenantId());
        assertEquals("retail-tenant", result.content().get(0).targetTenantId());
        assertEquals("ACTIVE", result.content().get(0).permissionStatus());
        verify(distributedPassportQueryPort).findByTargetTenant("retail-tenant", 0, 20, "SN", null);
    }

    @Test
    void getDistributedPassportDetail_returnsRetailReadableProductDetail() {
        Instant manufacturedAt = Instant.parse("2025-02-01T10:00:00Z");
        when(distributedPassportQueryPort.findDetailByRetailAccess("retail-tenant", "passport-1")).thenReturn(
            new DistributedPassportDetailView(
                "passport-1",
                "QR-001",
                "SN-001",
                "MODEL-1",
                "Model Name",
                "ACTIVE",
                "NONE",
                manufacturedAt,
                "BATCH-01",
                "FACTORY-A"
            )
        );

        DistributedPassportDetailView result = service.getDistributedPassportDetail(
            "retail-tenant",
            "passport-1"
        );

        assertEquals("passport-1", result.passportId());
        assertEquals("QR-001", result.qrPublicCode());
        assertEquals("SN-001", result.serialNumber());
        assertEquals("MODEL-1", result.modelId());
        assertEquals("Model Name", result.modelName());
        assertEquals("ACTIVE", result.assetState());
        assertEquals("NONE", result.riskFlag());
        assertEquals(manufacturedAt, result.manufacturedAt());
        assertEquals("BATCH-01", result.productionBatch());
        assertEquals("FACTORY-A", result.factoryCode());
        verify(distributedPassportQueryPort).findDetailByRetailAccess("retail-tenant", "passport-1");
    }

    private static ProductProperties productProperties() {
        ProductProperties props = new ProductProperties();
        props.setPublicBaseUrl("https://public.example.com");
        return props;
    }
}
