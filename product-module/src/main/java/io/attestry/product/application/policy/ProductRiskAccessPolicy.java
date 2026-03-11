package io.attestry.product.application.policy;

import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.port.ownership.PassportOwnershipPort;
import io.attestry.product.application.port.auth.ProductAuthorizationPort;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.ownership.model.PassportOwnership;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductRiskAccessPolicy {

    private final PassportOwnershipPort ownershipPort;
    private final ProductAuthorizationPort productAuthorizationPort;

    public void assertFlagAllowed(ProductActor actor, String passportId) {
        productAuthorizationPort.assertOwnerRiskFlagAllowed(actor);
        assertOwnership(actor, passportId);
    }

    public void assertClearAllowed(ProductActor actor, String passportId) {
        productAuthorizationPort.assertOwnerRiskClearAllowed(actor);
        assertOwnership(actor, passportId);
    }

    private void assertOwnership(ProductActor actor, String passportId) {
        PassportOwnership ownership = ownershipPort.findByPassportId(passportId)
            .orElseThrow(() -> new ProductDomainException(
                ProductErrorCode.NOT_ASSET_OWNER,
                "No ownership record for passport: " + passportId
            ));
        if (!actor.userId().equals(ownership.getOwnerId())) {
            throw new ProductDomainException(
                ProductErrorCode.NOT_ASSET_OWNER,
                "Actor is not the owner of passport: " + passportId
            );
        }
    }
}
