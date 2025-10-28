package fr.airsen.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommuneDetailResponse(
    String inseeCode,
    String name,
    String departmentCode,
    String departmentName,
    String regionCode,
    String regionName,
    Double latitude,
    Double longitude,
    Long population,
    AirQualityData airQuality,
    WeatherData weather
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AirQualityData(
        Integer atmoIndex,
        String qualifier,
        String color,
        PollutantData pollutants,
        LocalDateTime measurementDate,
        String source,
        LocalDateTime updateDate
    ) {}

    public record PollutantData(
        Integer no2,
        Integer o3,
        Integer pm10,
        Integer pm25,
        Integer so2
    ) {}
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record WeatherData(
        Double temperature,
        Integer humidity,
        Double windSpeed,
        Integer windDirection,
        Integer weatherCode,
        String weatherDescription,
        String timezone,
        LocalDateTime timestamp
    ) {}
}
