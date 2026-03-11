package io.attestry.product.infrastructure.persistence.jpa.entity;

import java.io.Serializable;
import java.util.Objects;

public class ShipmentEvidenceProjectionId implements Serializable {

    private String shipmentId;
    private String evidenceId;

    public ShipmentEvidenceProjectionId() {
    }

    public ShipmentEvidenceProjectionId(String shipmentId, String evidenceId) {
        this.shipmentId = shipmentId;
        this.evidenceId = evidenceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShipmentEvidenceProjectionId that)) return false;
        return Objects.equals(shipmentId, that.shipmentId)
            && Objects.equals(evidenceId, that.evidenceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shipmentId, evidenceId);
    }
}
