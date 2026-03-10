package io.attestry.product.interfaces.http.dto.request;

public record VoidRequest(
    String reason,
    String note
) {
}
