package io.attestry.product.application.port.passport;

import io.attestry.product.domain.passport.model.VoidReason;

public interface VoidCommandPort {

    void voidAsset(String passportId, VoidReason reason, String note);
}
