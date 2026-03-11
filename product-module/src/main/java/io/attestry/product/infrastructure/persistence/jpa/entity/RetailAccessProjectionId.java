package io.attestry.product.infrastructure.persistence.jpa.entity;

import java.io.Serializable;
import java.util.Objects;

public class RetailAccessProjectionId implements Serializable {

    private String tenantId;
    private String passportId;
    private String accessSourceType;
    private String accessSourceId;

    public RetailAccessProjectionId() {
    }

    public RetailAccessProjectionId(String tenantId, String passportId, String accessSourceType, String accessSourceId) {
        this.tenantId = tenantId;
        this.passportId = passportId;
        this.accessSourceType = accessSourceType;
        this.accessSourceId = accessSourceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RetailAccessProjectionId that)) return false;
        return Objects.equals(tenantId, that.tenantId)
            && Objects.equals(passportId, that.passportId)
            && Objects.equals(accessSourceType, that.accessSourceType)
            && Objects.equals(accessSourceId, that.accessSourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, passportId, accessSourceType, accessSourceId);
    }
}
