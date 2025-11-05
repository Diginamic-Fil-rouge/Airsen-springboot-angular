package fr.airsen.api.mapper;

import fr.airsen.api.dto.response.NearestWeatherResult;
import fr.airsen.api.dto.response.WeatherResponse;
import fr.airsen.api.entity.WeatherData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WeatherMapper {

    /**
     * Map WeatherData entity to direct response (data from requested commune)
     */
    @Mapping(target = "inseeCode", source = "commune.inseeCode")
    @Mapping(target = "communeName", source = "commune.name")
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
    @Mapping(target = "dataQualityNote",
        expression = "java(String.format(\"Données estimées depuis %s (%.1f km)\", result.communeName(), result.distanceKm()))")
    WeatherResponse toEstimatedResponse(NearestWeatherResult result);
}

