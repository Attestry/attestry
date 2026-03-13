package io.attestry.product.application.dto.result;

public record BatchMintError(
    int row,
    String serialNumber,
    String reason
) {
}
