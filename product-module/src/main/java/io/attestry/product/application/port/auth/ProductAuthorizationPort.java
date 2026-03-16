package io.attestry.product.application.port.auth;

import io.attestry.product.application.dto.command.ProductActor;

public interface ProductAuthorizationPort {

    void assertBrandMintAllowed(ProductActor actor, String tenantId, String serialNumber);

    void assertBrandVoidAllowed(ProductActor actor, String tenantId, String passportId);

    void assertOwnerRiskFlagAllowed(ProductActor actor);

    void assertOwnerRiskClearAllowed(ProductActor actor);

    void assertOwnerRetireAllowed(ProductActor actor);

    void assertPassportPermissionGrantAllowed(ProductActor actor);
}
