package io.attestry.product.application.dto.result;

import java.util.List;

public record BatchMintResult(
    int totalRequested,
    int totalMinted,
    int totalFailed,
    List<BatchMintError> errors
) {
}
