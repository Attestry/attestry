package io.attestry.product.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "product_passport_shipment_projection")
public class PassportShipmentProjectionJpaEntity {

    @Id
    @Column(name = "passport_id", nullable = false, length = 36)
    private String passportId;

    @Column(name = "shipment_id", nullable = false, unique = true, length = 36)
    private String shipmentId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "shipment_round", nullable = false)
    private int shipmentRound;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "released_by_user_display", length = 255)
    private String releasedByUserDisplay;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "returned_by_user_display", length = 255)
    private String returnedByUserDisplay;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PassportShipmentProjectionJpaEntity() {
    }

    public String getPassportId() { return passportId; }
    public String getShipmentId() { return shipmentId; }
    public String getStatus() { return status; }
    public int getShipmentRound() { return shipmentRound; }
    public Instant getReleasedAt() { return releasedAt; }
    public String getReleasedByUserDisplay() { return releasedByUserDisplay; }
    public Instant getReturnedAt() { return returnedAt; }
    public String getReturnedByUserDisplay() { return returnedByUserDisplay; }
    public Instant getUpdatedAt() { return updatedAt; }
}
