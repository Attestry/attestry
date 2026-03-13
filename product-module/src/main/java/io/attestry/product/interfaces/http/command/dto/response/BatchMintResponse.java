package io.attestry.product.interfaces.http.command.dto.response;

import io.attestry.product.application.dto.result.BatchMintResult;
import java.util.List;

public record BatchMintResponse(
    int totalRequested,
    int totalMinted,
    int totalFailed,
    List<BatchMintErrorResponse> errors
) {
    public static BatchMintResponse from(BatchMintResult result) {
        List<BatchMintErrorResponse> errors = result.errors().stream()
            .map(e -> new BatchMintErrorResponse(e.row(), e.serialNumber(), e.reason()))
            .toList();
        return new BatchMintResponse(result.totalRequested(), result.totalMinted(), result.totalFailed(), errors);
    }

    public record BatchMintErrorResponse(int row, String serialNumber, String reason) {
    }
}
