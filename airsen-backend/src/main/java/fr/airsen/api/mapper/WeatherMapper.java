package fr.airsen.api.mapper;

import fr.airsen.api.dto.response.NearestWeatherResult;
import fr.airsen.api.dto.response.WeatherResponse;
import fr.airsen.api.entity.WeatherData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface WeatherMapper {

    /**
     * Map WeatherData entity to direct response (data from requested commune)
     */
    @Mapping(target = "inseeCode", source = "commune.inseeCode")
    @Mapping(target = "communeName", source = "commune.name")
    @Mapping(target = "weatherDescription", source = "weatherCode", qualifiedByName = "weatherCodeToDescription")
    @Mapping(target = "dataSource", constant = "DIRECT")
    @Mapping(target = "estimatedFromCommune", ignore = true)
    @Mapping(target = "distanceKm", ignore = true)
    @Mapping(target = "dataQualityNote", expression = "java(\"Données mesurées pour cette commune\")")
    WeatherResponse toDirectResponse(WeatherData weatherData);

    /**
     * Map NearestWeatherResult to estimated response (data from nearest commune)
     */
    @Mapping(target = "dataSource", constant = "ESTIMATED")
    @Mapping(target = "estimatedFromCommune", source = "communeName")
    @Mapping(target = "weatherDescription", source = "weatherCode", qualifiedByName = "weatherCodeToDescription")
    @Mapping(target = "dataQualityNote",
        expression = "java(String.format(\"Données estimées depuis %s (%.1f km)\", result.communeName(), result.distanceKm()))")
    WeatherResponse toEstimatedResponse(NearestWeatherResult result);

    @Named("weatherCodeToDescription")
    default String weatherCodeToDescription(Integer weatherCode) {
        if (weatherCode == null) {
            return "Unknown";
        }

        return switch (weatherCode) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Foggy";
            case 51, 53, 55 -> "Drizzle";
            case 61, 63, 65 -> "Rain";
            case 71, 73, 75 -> "Snow";
            case 77 -> "Snow grains";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Weather code " + weatherCode;
        };
    }
}
