package io.attestry.workflow.interfaces.transfer.dto.request;

public record AcceptTransferRequest(
        String qrNonce,
        String password
) {
}
