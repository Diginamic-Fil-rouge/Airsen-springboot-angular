package fr.airsen.api.mapper;

import fr.airsen.api.dto.response.CommuneDetailResponse;
import fr.airsen.api.entity.AirQuality;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.entity.WeatherData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface CommuneDetailMapper {

    @Mapping(source = "commune.inseeCode", target = "inseeCode")
    @Mapping(source = "commune.name", target = "name")
    @Mapping(source = "commune.departmentCode", target = "departmentCode")
    @Mapping(source = "commune.department.name", target = "departmentName")
    @Mapping(source = "commune.regionCode", target = "regionCode")
    @Mapping(source = "commune.department.region.name", target = "regionName")
    @Mapping(source = "commune.latitude", target = "latitude", qualifiedByName = "bigDecimalToDouble")
    @Mapping(source = "commune.longitude", target = "longitude", qualifiedByName = "bigDecimalToDouble")
    @Mapping(source = "commune.population", target = "population")
    @Mapping(source = "airQuality", target = "airQuality")
    @Mapping(source = "weatherData", target = "weather")
    CommuneDetailResponse toCommuneDetailResponse(
        Commune commune,
        AirQuality airQuality,
        WeatherData weatherData
    );

    @Mapping(source = "atmoIndex", target = "atmoIndex")
    @Mapping(source = "qualifier", target = "qualifier")
    @Mapping(source = "color", target = "color")
    @Mapping(source = "airQuality", target = "pollutants")
    @Mapping(source = "measurementDate", target = "measurementDate", qualifiedByName = "localDateToLocalDateTime")
    @Mapping(target = "source", constant = "Database")
    @Mapping(source = "createdAt", target = "updateDate", qualifiedByName = "localDateToLocalDateTime")
    CommuneDetailResponse.AirQualityData toAirQualityData(AirQuality airQuality);

    @Mapping(source = "no2Concentration", target = "no2")
    @Mapping(source = "o3Concentration", target = "o3")
    @Mapping(source = "pm10Concentration", target = "pm10")
    @Mapping(source = "pm25Concentration", target = "pm25")
    @Mapping(source = "so2Concentration", target = "so2")
    CommuneDetailResponse.PollutantData toPollutantData(AirQuality airQuality);

    @Mapping(source = "temperature", target = "temperature")
    @Mapping(source = "humidity", target = "humidity")
    @Mapping(source = "windSpeed", target = "windSpeed")
    @Mapping(source = "windDirection", target = "windDirection")
    @Mapping(source = "weatherCode", target = "weatherCode")
    @Mapping(source = "weatherCode", target = "weatherDescription", qualifiedByName = "weatherCodeToDescription")
    @Mapping(target = "timezone", constant = "Europe/Paris")
    @Mapping(source = "measurementDate", target = "timestamp", qualifiedByName = "localDateToLocalDateTime")
    CommuneDetailResponse.WeatherData toWeatherData(WeatherData weatherData);

    @Named("bigDecimalToDouble")
    default Double bigDecimalToDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    @Named("localDateToLocalDateTime")
    default LocalDateTime localDateToLocalDateTime(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

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
