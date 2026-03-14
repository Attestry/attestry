package io.attestry.product.interfaces.http.command.dto.request;

public record VoidRequest(
    String reason,
    String note
) {
}
