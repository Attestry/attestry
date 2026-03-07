package io.attestry.workflow.application.shipment;

import io.attestry.product.application.port.PassportShipmentQueryPort;
import io.attestry.userauth.application.port.ObjectStoragePort;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.port.WorkflowEvidencePort;
import io.attestry.workflow.application.port.ShipmentProductReadPort;
import io.attestry.workflow.application.shipment.result.ShipmentDetailResult;
import io.attestry.workflow.application.shipment.result.ShipmentReleaseCandidateResult;
import io.attestry.workflow.application.shipment.result.ShipmentViewResult;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.ShipmentQueryUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.shipment.model.Shipment;
import io.attestry.workflow.domain.shipment.repository.ShipmentRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentQueryService implements ShipmentQueryUseCase, PassportShipmentQueryPort {

    private static final Duration DOWNLOAD_TTL = Duration.ofMinutes(30);

    private final ShipmentRepository shipmentRepository;
    private final WorkflowEvidencePort evidencePort;
    private final ShipmentProductReadPort shipmentProductReadPort;
    private final ObjectStoragePort objectStoragePort;
    private final WorkflowAuthorizationSupport authorizationSupport;

    public ShipmentQueryService(
            ShipmentRepository shipmentRepository,
            WorkflowEvidencePort evidencePort,
            ShipmentProductReadPort shipmentProductReadPort,
            ObjectStoragePort objectStoragePort,
            WorkflowAuthorizationSupport authorizationSupport) {
        this.shipmentRepository = shipmentRepository;
        this.evidencePort = evidencePort;
        this.shipmentProductReadPort = shipmentProductReadPort;
        this.objectStoragePort = objectStoragePort;
        this.authorizationSupport = authorizationSupport;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentViewResult> listByPassport(
            AuthPrincipal principal,
            String passportId) {
        String tenantId = principal.tenantId();
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.BRAND_RELEASE,
                "shipment:list:" + passportId);
        return shipmentRepository.findByPassportId(passportId).stream()
                .filter(shipment -> tenantId.equals(shipment.tenantId()))
                .map(this::toShipmentView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentViewResult> listByTenant(
            AuthPrincipal principal) {
        String tenantId = principal.tenantId();
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.TENANT_READ_ONLY,
                "shipment:list-all");
        return shipmentRepository.findByTenantId(tenantId).stream()
                .map(this::toShipmentView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentDetailResult getShipmentDetail(
            AuthPrincipal principal,
            String shipmentId) {
        String tenantId = principal.tenantId();
        authorizationSupport.assertTenantContext(principal, tenantId);
        Shipment shipment = shipmentRepository.findByShipmentId(shipmentId)
                .orElseThrow(
                        () -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Shipment not found"));
        if (!tenantId.equals(shipment.tenantId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION,
                    "Cross-tenant shipment access denied");
        }
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.TENANT_READ_ONLY,
                "shipment:view:" + shipmentId);

        List<ShipmentDetailResult.EvidenceFileResult> releaseFiles = toPassportEvidenceFiles(shipment.evidenceGroupId()).stream()
                .map(f -> new ShipmentDetailResult.EvidenceFileResult(
                        f.evidenceId(), f.originalFileName(), f.contentType(), f.sizeBytes(), f.downloadUrl()))
                .toList();
        List<ShipmentDetailResult.EvidenceFileResult> returnFiles = toPassportEvidenceFiles(shipment.returnEvidenceGroupId()).stream()
                .map(f -> new ShipmentDetailResult.EvidenceFileResult(
                        f.evidenceId(), f.originalFileName(), f.contentType(), f.sizeBytes(), f.downloadUrl()))
                .toList();

        return new ShipmentDetailResult(
                shipment.shipmentId(),
                shipment.tenantId(),
                shipment.passportId(),
                shipment.shipmentRound(),
                shipment.status().name(),
                shipment.releasedAt(),
                shipment.releasedByUserId(),
                shipment.returnedAt(),
                shipment.returnedByUserId(),
                releaseFiles,
                returnFiles,
                shipment.createdAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentReleaseCandidateResult> listReleaseCandidates(AuthPrincipal principal) {
        String tenantId = principal.tenantId();
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.TENANT_READ_ONLY,
                "shipment:release-candidates");
        return shipmentProductReadPort.findReleaseCandidatesByTenantId(tenantId).stream()
                .map(candidate -> new ShipmentReleaseCandidateResult(
                        candidate.passportId(),
                        candidate.assetId(),
                        candidate.serialNumber(),
                        candidate.modelId(),
                        candidate.modelName(),
                        candidate.productionBatch(),
                        candidate.factoryCode()))
                .toList();
    }

    // --- PassportShipmentQueryPort ---

    @Override
    @Transactional(readOnly = true)
    public Optional<ShipmentView> findLatestShipmentByPassportId(String passportId) {
        return shipmentRepository.findLatestByPassportId(passportId)
                .map(shipment -> {
                    List<EvidenceFileView> releaseFiles = toPassportEvidenceFiles(shipment.evidenceGroupId());
                    List<EvidenceFileView> returnFiles = toPassportEvidenceFiles(shipment.returnEvidenceGroupId());
                    List<EvidenceFileView> allFiles = new java.util.ArrayList<>(releaseFiles);
                    allFiles.addAll(returnFiles);
                    return new ShipmentView(
                            shipment.shipmentId(),
                            shipment.status().name(),
                            shipment.shipmentRound(),
                            shipment.releasedAt(),
                            shipment.releasedByUserId(),
                            shipment.returnedAt(),
                            shipment.returnedByUserId(),
                            allFiles);
                });
    }

    private List<EvidenceFileView> toPassportEvidenceFiles(String evidenceGroupId) {
        if (evidenceGroupId == null) return List.of();
        return evidencePort.findEvidenceByEvidenceGroupId(evidenceGroupId).stream()
                .filter(e -> "READY".equals(e.status()))
                .map(e -> {
                    String downloadUrl = e.objectKey() != null
                            ? objectStoragePort.issuePresignedDownload(e.objectKey(), DOWNLOAD_TTL).downloadUrl()
                            : null;
                    return new EvidenceFileView(
                            e.evidenceId(), e.originalFileName(), e.contentType(), e.sizeBytes(), downloadUrl);
                })
                .toList();
    }

    // --- private helpers ---

    private ShipmentViewResult toShipmentView(Shipment shipment) {
        return new ShipmentViewResult(
                shipment.shipmentId(),
                shipment.tenantId(),
                shipment.passportId(),
                shipment.shipmentRound(),
                shipment.status().name(),
                shipment.releasedAt(),
                shipment.releasedByUserId(),
                shipment.releasedByTenantId(),
                shipment.evidenceGroupId(),
                shipment.returnedAt(),
                shipment.returnedByUserId(),
                shipment.returnEvidenceGroupId(),
                shipment.createdAt());
    }
}
