package io.attestry.kafka.workflow;

@FunctionalInterface
interface ProjectionRefreshHandler {

    boolean refresh(ProjectionEventContext context);
}
