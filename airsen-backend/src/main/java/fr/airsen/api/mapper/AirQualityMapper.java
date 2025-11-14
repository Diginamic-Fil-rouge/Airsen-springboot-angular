package fr.airsen.api.mapper;

import fr.airsen.api.dto.response.AirQualityResponse;
import fr.airsen.api.dto.response.NearestAirQualityResult;
import fr.airsen.api.entity.AirQuality;
import fr.airsen.api.entity.Commune;
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
     *
     * @param result Air quality data from nearest commune
     * @param requestedCommune The commune that was originally requested
     * @return AirQualityResponse with requested commune's ID/name and ESTIMATED data from nearest
     */
    @Mapping(target = "inseeCode", source = "requestedCommune.inseeCode")
    @Mapping(target = "communeName", source = "requestedCommune.name")
    @Mapping(target = "measurementDate", source = "result.measurementDate")
    @Mapping(target = "atmoIndex", source = "result.atmoIndex")
    @Mapping(target = "qualifier", source = "result.qualifier")
    @Mapping(target = "color", source = "result.color")
    @Mapping(target = "pollutants", expression = "java(mapPollutants(result))")
    @Mapping(target = "dataSource", constant = "ESTIMATED")
    @Mapping(target = "estimatedFromCommune", source = "result.communeName")
    @Mapping(target = "distanceKm", source = "result.distanceKm")
    @Mapping(target = "dataQualityNote",
        expression = "java(String.format(\"Données estimées depuis %s (%.1f km)\", result.communeName(), result.distanceKm()))")
    AirQualityResponse toEstimatedResponse(NearestAirQualityResult result, Commune requestedCommune);

    /**
     * Helper method to map pollutant values from AirQuality entity to Map
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

    /**
     * Helper method to map pollutant values from NearestAirQualityResult to Map
     */
    default Map<String, Integer> mapPollutants(NearestAirQualityResult result) {
        Map<String, Integer> pollutants = new HashMap<>();
        pollutants.put("NO2", result.no2());
        pollutants.put("O3", result.o3());
        pollutants.put("PM10", result.pm10());
        pollutants.put("PM25", result.pm25());
        pollutants.put("SO2", result.so2());
        return pollutants;
    }
}

