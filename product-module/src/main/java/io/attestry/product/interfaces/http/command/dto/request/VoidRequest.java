package io.attestry.product.interfaces.http.command.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VoidRequest(
    @NotBlank(message = "Reason is required")
    String reason,

    String note
) {
}
