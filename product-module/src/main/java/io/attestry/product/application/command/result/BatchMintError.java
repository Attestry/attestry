package io.attestry.product.application.command.result;

public record BatchMintError(
    int row,
    String serialNumber,
    String reason
) {
}
