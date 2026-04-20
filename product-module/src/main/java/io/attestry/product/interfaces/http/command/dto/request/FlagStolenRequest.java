package io.attestry.product.interfaces.http.command.dto.request;

import jakarta.validation.constraints.Size;

public record FlagStolenRequest(
    @Size(max = 100) String policeReportNo
) {
}
