package io.attestry.product.application.command.result;

import java.util.List;

public record BatchMintResult(
    int totalRequested,
    int totalMinted,
    int totalFailed,
    List<BatchMintError> errors
) {
}
