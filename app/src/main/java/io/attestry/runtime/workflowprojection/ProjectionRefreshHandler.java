package io.attestry.runtime.workflowprojection;

@FunctionalInterface
interface ProjectionRefreshHandler {

    boolean refresh(ProjectionEventContext context);
}
