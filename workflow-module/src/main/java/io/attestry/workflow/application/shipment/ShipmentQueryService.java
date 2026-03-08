package io.attestry.workflow.application.shipment;

import io.attestry.product.application.port.PassportShipmentQueryPort;
import io.attestry.userauth.application.port.ObjectStoragePort;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.port.WorkflowEvidencePort;
import io.attestry.workflow.application.port.ShipmentProductReadPort;
import io.attestry.workflow.application.port.UserReadPort;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentQueryService implements ShipmentQueryUseCase, PassportShipmentQueryPort {

        private static final Duration DOWNLOAD_TTL = Duration.ofMinutes(30);

        private final ShipmentRepository shipmentRepository;
        private final WorkflowEvidencePort evidencePort;
        private final ShipmentProductReadPort shipmentProductReadPort;
        private final ObjectStoragePort objectStoragePort;
        private final UserReadPort userReadPort;
        private final WorkflowAuthorizationSupport authorizationSupport;

        public ShipmentQueryService(
                        ShipmentRepository shipmentRepository,
                        WorkflowEvidencePort evidencePort,
                        ShipmentProductReadPort shipmentProductReadPort,
                        ObjectStoragePort objectStoragePort,
                        UserReadPort userReadPort,
                        WorkflowAuthorizationSupport authorizationSupport) {
                this.shipmentRepository = shipmentRepository;
                this.evidencePort = evidencePort;
                this.shipmentProductReadPort = shipmentProductReadPort;
                this.objectStoragePort = objectStoragePort;
                this.userReadPort = userReadPort;
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
                List<Shipment> shipments = shipmentRepository.findByPassportId(passportId).stream()
                                .filter(shipment -> tenantId.equals(shipment.tenantId()))
                                .toList();
                return enrichShipments(shipments);
        }

        @Override
        @Transactional(readOnly = true)
        public PagedShipmentViewResponse listByTenant(
                        AuthPrincipal principal,
                        int page,
                        int size,
                        String keyword) {
                String tenantId = principal.tenantId();
                authorizationSupport.assertTenantContext(principal, tenantId);
                authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.TENANT_READ_ONLY,
                                "shipment:list-all");
                ShipmentProductReadPort.PagedShipmentViewResult result = shipmentProductReadPort
                                .findShipmentsByTenantId(tenantId, page, size, keyword);
                List<ShipmentViewResult> content = result.content().stream()
                                .map(v -> new ShipmentViewResult(
                                                v.shipmentId(), v.tenantId(), v.passportId(),
                                                v.assetId(), v.serialNumber(), v.modelId(),
                                                v.modelName(), v.productionBatch(), v.factoryCode(),
                                                v.shipmentRound(), v.status(), v.releasedAt(),
                                                v.releasedByUserId(), v.releasedByTenantId(),
                                                v.evidenceGroupId(), v.returnedAt(),
                                                v.returnedByUserId(), v.returnEvidenceGroupId(),
                                                v.createdAt()))
                                .toList();
                return new PagedShipmentViewResponse(
                                content, result.page(), result.size(),
                                result.totalElements(), result.totalPages());
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
                                                () -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST,
                                                                "Shipment not found"));
                if (!tenantId.equals(shipment.tenantId())) {
                        throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION,
                                        "Cross-tenant shipment access denied");
                }
                authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.TENANT_READ_ONLY,
                                "shipment:view:" + shipmentId);

                List<ShipmentDetailResult.EvidenceFileResult> releaseFiles = toPassportEvidenceFiles(
                                shipment.evidenceGroupId()).stream()
                                .map(f -> new ShipmentDetailResult.EvidenceFileResult(
                                                f.evidenceId(), f.originalFileName(), f.contentType(), f.sizeBytes(),
                                                f.downloadUrl()))
                                .toList();
                List<ShipmentDetailResult.EvidenceFileResult> returnFiles = toPassportEvidenceFiles(
                                shipment.returnEvidenceGroupId()).stream()
                                .map(f -> new ShipmentDetailResult.EvidenceFileResult(
                                                f.evidenceId(), f.originalFileName(), f.contentType(), f.sizeBytes(),
                                                f.downloadUrl()))
                                .toList();

                ShipmentProductReadPort.PassportAssetInfo assetInfo = shipmentProductReadPort
                                .findPassportAssetInfoByIds(List.of(shipment.passportId()))
                                .get(shipment.passportId());

                List<String> userIds = Stream
                                .of(shipment.releasedByUserId(), shipment.returnedByUserId())
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList();
                Map<String, String> emailMap = userReadPort.findEmailsByUserIds(userIds);

                return new ShipmentDetailResult(
                                shipment.shipmentId(),
                                shipment.tenantId(),
                                shipment.passportId(),
                                assetInfo != null ? assetInfo.modelName() : "",
                                assetInfo != null ? assetInfo.serialNumber() : "",
                                shipment.shipmentRound(),
                                shipment.status().name(),
                                shipment.releasedAt(),
                                emailMap.getOrDefault(shipment.releasedByUserId(), null),
                                shipment.returnedAt(),
                                emailMap.getOrDefault(shipment.returnedByUserId(), null),
                                releaseFiles,
                                returnFiles,
                                shipment.createdAt());
        }

        @Override
        @Transactional(readOnly = true)
        public PagedReleaseCandidateResponse listReleaseCandidates(
                        AuthPrincipal principal,
                        int page,
                        int size,
                        String keyword) {
                String tenantId = principal.tenantId();
                authorizationSupport.assertTenantContext(principal, tenantId);
                authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.TENANT_READ_ONLY,
                                "shipment:release-candidates");
                ShipmentProductReadPort.PagedReleaseCandidateResult result = shipmentProductReadPort
                                .findReleaseCandidatesByTenantId(tenantId, page, size, keyword);
                List<ShipmentReleaseCandidateResult> content = result.content().stream()
                                .map(candidate -> new ShipmentReleaseCandidateResult(
                                                candidate.passportId(),
                                                candidate.assetId(),
                                                candidate.serialNumber(),
                                                candidate.modelId(),
                                                candidate.modelName(),
                                                candidate.productionBatch(),
                                                candidate.factoryCode()))
                                .toList();
                return new PagedReleaseCandidateResponse(
                                content, result.page(), result.size(),
                                result.totalElements(), result.totalPages());
        }

        // --- PassportShipmentQueryPort ---

        @Override
        @Transactional(readOnly = true)
        public Optional<ShipmentView> findLatestShipmentByPassportId(String passportId) {
                return shipmentRepository.findLatestByPassportId(passportId)
                                .map(shipment -> {
                                        List<EvidenceFileView> releaseFiles = toPassportEvidenceFiles(
                                                        shipment.evidenceGroupId());
                                        List<EvidenceFileView> returnFiles = toPassportEvidenceFiles(
                                                        shipment.returnEvidenceGroupId());
                                        List<EvidenceFileView> allFiles = new java.util.ArrayList<>(releaseFiles);
                                        allFiles.addAll(returnFiles);
                                        List<String> userIds = Stream
                                                        .of(shipment.releasedByUserId(), shipment.returnedByUserId())
                                                        .filter(Objects::nonNull)
                                                        .distinct()
                                                        .toList();
                                        Map<String, String> emailMap = userReadPort.findEmailsByUserIds(userIds);
                                        return new ShipmentView(
                                                        shipment.shipmentId(),
                                                        shipment.status().name(),
                                                        shipment.shipmentRound(),
                                                        shipment.releasedAt(),
                                                        emailMap.getOrDefault(shipment.releasedByUserId(), null),
                                                        shipment.returnedAt(),
                                                        emailMap.getOrDefault(shipment.returnedByUserId(), null),
                                                        allFiles);
                                });
        }

        private List<EvidenceFileView> toPassportEvidenceFiles(String evidenceGroupId) {
                if (evidenceGroupId == null)
                        return List.of();
                return evidencePort.findEvidenceByEvidenceGroupId(evidenceGroupId).stream()
                                .filter(e -> "READY".equals(e.status()))
                                .map(e -> {
                                        String downloadUrl = e.objectKey() != null
                                                        ? objectStoragePort.issuePresignedDownload(e.objectKey(),
                                                                        DOWNLOAD_TTL).downloadUrl()
                                                        : null;
                                        return new EvidenceFileView(
                                                        e.evidenceId(), e.originalFileName(), e.contentType(),
                                                        e.sizeBytes(), downloadUrl);
                                })
                                .toList();
        }

        // --- private helpers ---

        private List<ShipmentViewResult> enrichShipments(List<Shipment> shipments) {
                if (shipments.isEmpty())
                        return List.of();
                List<String> passportIds = shipments.stream().map(Shipment::passportId).distinct().toList();
                Map<String, ShipmentProductReadPort.PassportAssetInfo> assetMap = shipmentProductReadPort
                                .findPassportAssetInfoByIds(passportIds);
                return shipments.stream()
                                .map(shipment -> {
                                        ShipmentProductReadPort.PassportAssetInfo asset = assetMap
                                                        .get(shipment.passportId());
                                        return new ShipmentViewResult(
                                                        shipment.shipmentId(),
                                                        shipment.tenantId(),
                                                        shipment.passportId(),
                                                        asset != null ? asset.assetId() : null,
                                                        asset != null ? asset.serialNumber() : null,
                                                        asset != null ? asset.modelId() : null,
                                                        asset != null ? asset.modelName() : null,
                                                        asset != null ? asset.productionBatch() : null,
                                                        asset != null ? asset.factoryCode() : null,
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
                                })
                                .toList();
        }
}
