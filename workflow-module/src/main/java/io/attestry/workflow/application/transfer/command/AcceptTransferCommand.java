package io.attestry.workflow.application.transfer.command;

public record AcceptTransferCommand(
    String qrNonce,
    String password
) {
}
