package io.attestry.kafka.workflow;

import java.util.Set;

final class ProductProjectionRefreshPolicy {

    private static final Set<ProductProjectionTrigger> STATE_AND_CATALOG_TRIGGERS = Set.of(
        new ProductProjectionTrigger(ProjectionEventCategory.GENESIS, ProjectionEventAction.MINTED),
        new ProductProjectionTrigger(ProjectionEventCategory.LIFECYCLE, ProjectionEventAction.VOIDED),
        new ProductProjectionTrigger(ProjectionEventCategory.LIFECYCLE, ProjectionEventAction.RETIRED)
    );

    private ProductProjectionRefreshPolicy() {
    }

    static boolean shouldRefreshStateAndCatalog(ProjectionEventCategory eventCategory, ProjectionEventAction eventAction) {
        return eventCategory == ProjectionEventCategory.RISK
            || STATE_AND_CATALOG_TRIGGERS.contains(new ProductProjectionTrigger(eventCategory, eventAction));
    }

    private record ProductProjectionTrigger(
        ProjectionEventCategory eventCategory,
        ProjectionEventAction eventAction
    ) {
    }
}
