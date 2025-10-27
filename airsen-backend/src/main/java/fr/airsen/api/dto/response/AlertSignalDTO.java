package fr.airsen.api.dto.response;

import fr.airsen.api.entity.enums.AlertSignalKind;
import fr.airsen.api.entity.enums.AlertSignalLevel;
import fr.airsen.api.entity.enums.AlertSignalSource;
import fr.airsen.api.entity.enums.GeographicScopeType;

import java.time.LocalDateTime;


public record AlertSignalDTO(
    Long id,
    AlertSignalSource source,
    AlertSignalKind kind,
    AlertSignalLevel level,
    GeographicScopeType scopeType,
    Long scopeId,
    String summary,
    String details,
    LocalDateTime detectedAt,
    LocalDateTime validFrom,
    LocalDateTime validTo,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
