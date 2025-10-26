package fr.airsen.api.dto.request;

import fr.airsen.api.entity.enums.AlertSignalKind;
import fr.airsen.api.entity.enums.AlertSignalLevel;
import fr.airsen.api.entity.enums.AlertSignalSource;
import fr.airsen.api.entity.enums.GeographicScopeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Request DTO for creating a manual alert signal.
 *
 * Used by admins to create custom alert signals that are not automatically
 * detected from external APIs.
 */
public record CreateManualSignalRequest(

    @NotNull(message = "Alert source is required")
    AlertSignalSource source,

    @NotNull(message = "Alert kind is required")
    AlertSignalKind kind,

    @NotNull(message = "Alert level is required")
    AlertSignalLevel level,

    @NotNull(message = "Geographic scope type is required")
    GeographicScopeType scopeType,

    Long scopeId,

    @NotBlank(message = "Summary is required")
    @Size(max = 255, message = "Summary must not exceed 255 characters")
    String summary,

    String details,

    LocalDateTime validFrom,

    LocalDateTime validTo
) {
}
