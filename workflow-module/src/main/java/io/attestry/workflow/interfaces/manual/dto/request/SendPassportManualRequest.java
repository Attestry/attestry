package io.attestry.workflow.interfaces.manual.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SendPassportManualRequest(
    @NotEmpty(message = "Target passport IDs must not be empty.")
    List<String> passportIds,
    @Size(max = 2000, message = "Manual content must be 2000 characters or less.")
    String message,
    String evidenceGroupId
) {
}
