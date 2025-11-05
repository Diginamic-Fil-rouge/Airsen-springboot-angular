package fr.airsen.api.mapper;

import fr.airsen.api.dto.response.AirQualityResponse;
import fr.airsen.api.dto.response.NearestAirQualityResult;
import fr.airsen.api.entity.AirQuality;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface AirQualityMapper {

    /**
     * Map AirQuality entity to direct response (data from requested commune)
     */
    @Mapping(target = "inseeCode", source = "commune.inseeCode")
    @Mapping(target = "communeName", source = "commune.name")
    @Mapping(target = "atmoIndex", source = "atmIndex")
    @Mapping(target = "qualifier", source = "atmoQual")
    @Mapping(target = "color", source = "atmoColor")
    @Mapping(target = "pollutants", expression = "java(mapPollutants(airQuality))")
    @Mapping(target = "dataSource", constant = "DIRECT")
    @Mapping(target = "estimatedFromCommune", ignore = true)
    @Mapping(target = "distanceKm", ignore = true)
    @Mapping(target = "dataQualityNote", expression = "java(\"Données mesurées pour cette commune\")")
    AirQualityResponse toDirectResponse(AirQuality airQuality);

    /**
     * Map NearestAirQualityResult to estimated response
     */
    @Mapping(target = "dataSource", constant = "ESTIMATED")
    @Mapping(target = "estimatedFromCommune", source = "communeName")
    @Mapping(target = "dataQualityNote",
        expression = "java(String.format(\"Données estimées depuis %s (%.1f km)\", result.communeName(), result.distanceKm()))")
    AirQualityResponse toEstimatedResponse(NearestAirQualityResult result);

    /**
     * Helper method to map pollutant values to Map
     */
    default Map<String, Integer> mapPollutants(AirQuality airQuality) {
        Map<String, Integer> pollutants = new HashMap<>();
        pollutants.put("NO2", airQuality.getNo2Concentration());
        pollutants.put("O3", airQuality.getO3Concentration());
        pollutants.put("PM10", airQuality.getPm10Concentration());
        pollutants.put("PM25", airQuality.getPm25Concentration());
        pollutants.put("SO2", airQuality.getSo2Concentration());
        return pollutants;
    }
}

