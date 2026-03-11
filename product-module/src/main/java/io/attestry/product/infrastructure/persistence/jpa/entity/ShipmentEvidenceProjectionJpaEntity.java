package io.attestry.product.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "product_passport_shipment_evidence_projection")
@IdClass(ShipmentEvidenceProjectionId.class)
public class ShipmentEvidenceProjectionJpaEntity {

    @Id
    @Column(name = "shipment_id", nullable = false, length = 36)
    private String shipmentId;

    @Id
    @Column(name = "evidence_id", nullable = false, length = 48)
    private String evidenceId;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ShipmentEvidenceProjectionJpaEntity() {
    }

    public String getShipmentId() { return shipmentId; }
    public String getEvidenceId() { return evidenceId; }
    public String getOriginalFileName() { return originalFileName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getObjectKey() { return objectKey; }
}
