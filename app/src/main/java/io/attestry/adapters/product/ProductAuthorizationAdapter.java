package io.attestry.adapters.product;

import io.attestry.product.application.common.ProductActor;
import io.attestry.product.application.port.auth.ProductAuthorizationPort;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.userauth.contract.authorization.AuthorizationCheckPort;
import org.springframework.stereotype.Component;

@Component
public class ProductAuthorizationAdapter implements ProductAuthorizationPort {

    private static final String BRAND_MINT = "BRAND_MINT";
    private static final String BRAND_VOID = "BRAND_VOID";
    private static final String OWNER_RISK_FLAG = "OWNER_RISK_FLAG";
    private static final String OWNER_RISK_CLEAR = "OWNER_RISK_CLEAR";
    private static final String OWNER_RETIRE = "OWNER_RETIRE";
    private static final String PASSPORT_PERMISSION_GRANT = "PASSPORT_PERMISSION_GRANT";

    private final AuthorizationCheckPort authorizationCheckPort;

    public ProductAuthorizationAdapter(AuthorizationCheckPort authorizationCheckPort) {
        this.authorizationCheckPort = authorizationCheckPort;
    }

    @Override
    public void assertBrandMintAllowed(ProductActor actor, String tenantId, String serialNumber) {
        assertAllowed(
            actor,
            tenantId,
            BRAND_MINT,
            "mint:" + tenantId + ":" + serialNumber,
            AuthorizationCheckPort.DecisionMode.LIVE_RECHECK,
            ProductErrorCode.FORBIDDEN_MINT,
            "BRAND_MINT scope is required"
        );
    }

    @Override
    public void assertBrandVoidAllowed(ProductActor actor, String tenantId, String passportId) {
        assertAllowed(
            actor,
            tenantId,
            BRAND_VOID,
            "void:" + passportId,
            AuthorizationCheckPort.DecisionMode.LIVE_RECHECK,
            ProductErrorCode.FORBIDDEN_VOID,
            "BRAND_VOID scope is required"
        );
    }

    @Override
    public void assertOwnerRiskFlagAllowed(ProductActor actor) {
        assertAllowed(
            actor,
            actor.tenantId(),
            OWNER_RISK_FLAG,
            null,
            AuthorizationCheckPort.DecisionMode.TOKEN_SNAPSHOT,
            ProductErrorCode.FORBIDDEN_RISK_FLAG,
            "OWNER_RISK_FLAG scope is required"
        );
    }

    @Override
    public void assertOwnerRiskClearAllowed(ProductActor actor) {
        assertAllowed(
            actor,
            actor.tenantId(),
            OWNER_RISK_CLEAR,
            null,
            AuthorizationCheckPort.DecisionMode.TOKEN_SNAPSHOT,
            ProductErrorCode.FORBIDDEN_RISK_FLAG,
            "OWNER_RISK_CLEAR scope is required"
        );
    }

    @Override
    public void assertOwnerRetireAllowed(ProductActor actor) {
        assertAllowed(
            actor,
            actor.tenantId(),
            OWNER_RETIRE,
            null,
            AuthorizationCheckPort.DecisionMode.TOKEN_SNAPSHOT,
            ProductErrorCode.FORBIDDEN_RETIRE,
            "OWNER_RETIRE scope is required"
        );
    }

    @Override
    public void assertPassportPermissionGrantAllowed(ProductActor actor) {
        assertAllowed(
            actor,
            actor.tenantId(),
            PASSPORT_PERMISSION_GRANT,
            null,
            AuthorizationCheckPort.DecisionMode.LIVE_RECHECK,
            ProductErrorCode.FORBIDDEN_VOID,
            "PASSPORT_PERMISSION_GRANT scope is required"
        );
    }

    private void assertAllowed(
        ProductActor actor,
        String tenantId,
        String permissionCode,
        String resourceRef,
        AuthorizationCheckPort.DecisionMode decisionMode,
        ProductErrorCode errorCode,
        String message
    ) {
        AuthorizationCheckPort.AuthorizationDecision decision = authorizationCheckPort.authorize(
            new AuthorizationCheckPort.AuthorizationCheckCommand(
                actor.userId(),
                actor.tenantId(),
                tenantId,
                actor.scopes(),
                permissionCode,
                resourceRef,
                decisionMode
            )
        );
        if (!decision.allowed()) {
            throw new ProductDomainException(errorCode, message);
        }
    }
}
