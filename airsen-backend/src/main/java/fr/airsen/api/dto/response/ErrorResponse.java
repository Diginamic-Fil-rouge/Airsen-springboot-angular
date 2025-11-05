package fr.airsen.api.dto.response;

import java.time.LocalDateTime;

public record ErrorResponse(
    int status,
    String code,
    String message,
    LocalDateTime timestamp
) {}

