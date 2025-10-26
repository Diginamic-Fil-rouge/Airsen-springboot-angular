package fr.airsen.api.dto.response;

import java.time.LocalDateTime;
import java.util.Map;


public record AlertSignalStatisticsDTO(
    int totalSignals,
    Map<String, Integer> bySource,
    Map<String, Integer> byLevel,
    LocalDateTime lastDetectionAt
) {}
