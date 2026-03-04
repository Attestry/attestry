package io.attestry.product.application.port;

public interface BrandAccessValidationPort {
    void assertActiveBrandMembership(String actorUserId, String tenantId, String groupId);
}
