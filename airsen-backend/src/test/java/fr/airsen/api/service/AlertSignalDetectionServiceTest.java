package fr.airsen.api.service;

import fr.airsen.api.dto.request.CreateManualSignalRequest;
import fr.airsen.api.entity.AlertSignal;
import fr.airsen.api.entity.Region;
import fr.airsen.api.entity.enums.*;
import fr.airsen.api.external.client.AtmoApiClient;
import fr.airsen.api.external.client.OpenMeteoApiClient;
import fr.airsen.api.external.dto.atmo.AtmoAirQualityResponse;
import fr.airsen.api.external.dto.openmeteo.OpenMeteoForecastResponse;
import fr.airsen.api.repository.AlertSignalRepository;
import fr.airsen.api.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AlertSignalDetectionService.
 *
 * CRITICAL TESTS - Public Safety:
 * These threshold tests ensure alert signals are correctly triggered
 * to protect public health and safety. Incorrect thresholds could result in:
 * - Missed alerts during dangerous conditions
 * - False alarms causing alert fatigue
 * - GDPR violations from incorrect targeting
 *
 * Thresholds tested:
 * - Heat: >= 35.0°C (public health risk)
 * - Wind: >= 70.0 km/h (dangerous conditions)
 * - Rain: >= 30.0 mm (flooding risk)
 * - ATMO AQI: >= 4 (bad air quality)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertSignalDetectionService Unit Tests")
class AlertSignalDetectionServiceTest {

    @Mock
    private AlertSignalRepository alertSignalRepository;

    @Mock
    private AtmoApiClient atmoApiClient;

    @Mock
    private OpenMeteoApiClient openMeteoApiClient;

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private AlertSignalDetectionService detectionService;

    @Captor
    private ArgumentCaptor<AlertSignal> signalCaptor;

    private Region ileDeFranceRegion;

    @BeforeEach
    void setUp() {
        setupTestEntities();
    }

    private void setupTestEntities() {
        ileDeFranceRegion = new Region();
        ileDeFranceRegion.setId(11L);
        ileDeFranceRegion.setRegionCode("11");
        ileDeFranceRegion.setName("Île-de-France");
    }

    // ============================================
    // CRITICAL Test 1: ATMO AQI Threshold (>= 4)
    // ============================================

    @Test
    @DisplayName("CRITICAL: Should detect ATMO episode when AQI >= 4 (Bad)")
    void shouldDetectAtmoEpisodeWhenAQIEqualsFour() {
        // Given: ATMO data with AQI = 4 (Bad - threshold)
        AtmoAirQualityResponse badAirQuality = new AtmoAirQualityResponse(
            "75056",
            "Paris",
            "2025-11-16",
            4,  // CRITICAL: AQI = 4 (Bad) - should trigger alert
            "Mauvais",
            "#ff0000",
            4, 4, 4, 4, 4 // Pollutant codes
        );

        when(atmoApiClient.getCurrentAirQualityIndices())
            .thenReturn(Flux.just(badAirQuality));
        when(alertSignalRepository.save(any(AlertSignal.class)))
            .thenAnswer(inv -> {
                AlertSignal signal = inv.getArgument(0);
                signal.setId(1L);
                return signal;
            });

        // When
        List<AlertSignal> signals = detectionService.detectAtmoSignals();

        // Then
        assertThat(signals).hasSize(1);

        verify(alertSignalRepository).save(signalCaptor.capture());
        AlertSignal capturedSignal = signalCaptor.getValue();

        assertThat(capturedSignal.getSource()).isEqualTo(AlertSignalSource.ATMO);
        assertThat(capturedSignal.getKind()).isEqualTo(AlertSignalKind.AQI);
        assertThat(capturedSignal.getLevel()).isEqualTo(AlertSignalLevel.ALERT);
        assertThat(capturedSignal.getSummary()).contains("Air quality alert");
        assertThat(capturedSignal.getSummary()).contains("AQI 4");
        assertThat(capturedSignal.getDetails()).contains("Air quality index: 4");
        assertThat(capturedSignal.getDetails()).contains("Mauvais");
    }

    @Test
    @DisplayName("CRITICAL: Should NOT detect episode when AQI = 3 (Degraded - below threshold)")
    void shouldNotDetectAtmoEpisodeWhenAQIBelowThreshold() {
        // Given: ATMO data with AQI = 3 (Degraded - below threshold)
        AtmoAirQualityResponse degradedAirQuality = new AtmoAirQualityResponse(
            "75056",
            "Paris",
            "2025-11-16",
            3,  // AQI = 3 (Degraded) - should NOT trigger alert
            "Dégradé",
            "#ffcc00",
            3, 3, 3, 3, 3
        );

        when(atmoApiClient.getCurrentAirQualityIndices())
            .thenReturn(Flux.just(degradedAirQuality));

        // When
        List<AlertSignal> signals = detectionService.detectAtmoSignals();

        // Then
        assertThat(signals).isEmpty();
        verify(alertSignalRepository, never()).save(any());
    }

    @Test
    @DisplayName("CRITICAL: Should detect episode when AQI = 6 (Extremely Bad - well above threshold)")
    void shouldDetectAtmoEpisodeWhenAQIExtremelyBad() {
        // Given: ATMO data with AQI = 6 (Extremely Bad)
        AtmoAirQualityResponse extremelyBadAirQuality = new AtmoAirQualityResponse(
            "93008",
            "Saint-Denis",
            "2025-11-16",
            6,  // AQI = 6 (Extremely Bad) - should definitely trigger alert
            "Extrêmement mauvais",
            "#960032",
            6, 6, 6, 6, 6
        );

        when(atmoApiClient.getCurrentAirQualityIndices())
            .thenReturn(Flux.just(extremelyBadAirQuality));
        when(alertSignalRepository.save(any()))
            .thenAnswer(inv -> inv.getArgument(0));

        // When
        List<AlertSignal> signals = detectionService.detectAtmoSignals();

        // Then
        assertThat(signals).hasSize(1);
        verify(alertSignalRepository).save(any());
    }

    @Test
    @DisplayName("Should handle multiple ATMO episodes in single detection")
    void shouldHandleMultipleAtmoEpisodes() {
        // Given: Multiple communes with AQI >= 4
        AtmoAirQualityResponse episode1 = new AtmoAirQualityResponse(
            "75056", "Paris", "2025-11-16", 5, "Très mauvais", "#960032", 5, 5, 5, 5, 5
        );
        AtmoAirQualityResponse episode2 = new AtmoAirQualityResponse(
            "93008", "Saint-Denis", "2025-11-16", 4, "Mauvais", "#ff0000", 4, 4, 4, 4, 4
        );
        AtmoAirQualityResponse noEpisode = new AtmoAirQualityResponse(
            "92012", "Boulogne-Billancourt", "2025-11-16", 2, "Moyen", "#50ccaa", 2, 2, 2, 2, 2
        );

        when(atmoApiClient.getCurrentAirQualityIndices())
            .thenReturn(Flux.just(episode1, episode2, noEpisode));
        when(alertSignalRepository.save(any()))
            .thenAnswer(inv -> inv.getArgument(0));

        // When
        List<AlertSignal> signals = detectionService.detectAtmoSignals();

        // Then: Only 2 episodes should be detected (AQI 4 and 5, not 2)
        assertThat(signals).hasSize(2);
        verify(alertSignalRepository, times(2)).save(any());
    }

    // ============================================
    // CRITICAL Test 2: Heat Alert Threshold (>= 35°C)
    // ============================================

    @Test
    @DisplayName("CRITICAL: Should detect heat alert when temperature >= 35.0°C")
    void shouldDetectHeatAlertAtExactThreshold() {
        // Given: Weather forecast with max temperature = 35.0°C (exact threshold)
        OpenMeteoForecastResponse.DailyWeather daily = new OpenMeteoForecastResponse.DailyWeather(
            Arrays.asList("2025-11-16", "2025-11-17", "2025-11-18"),
            Arrays.asList(35.0, 32.0, 30.0),  // 35.0°C - CRITICAL threshold
            null,
            Arrays.asList(50.0, 45.0, 40.0),
            Arrays.asList(0.0, 0.0, 0.0)
        );
        OpenMeteoForecastResponse forecast = new OpenMeteoForecastResponse("Europe/Paris", daily);

        when(regionRepository.findAll()).thenReturn(Collections.singletonList(ileDeFranceRegion));
        when(openMeteoApiClient.getWeatherForecast(anyDouble(), anyDouble(), anyInt()))
            .thenReturn(Mono.just(forecast));
        when(alertSignalRepository.save(any()))
            .thenAnswer(inv -> {
                AlertSignal signal = inv.getArgument(0);
                signal.setId(10L);
                return signal;
            });

        // When
        List<AlertSignal> signals = detectionService.detectWeatherSignals();

        // Then
        assertThat(signals).hasSize(1);

        verify(alertSignalRepository).save(signalCaptor.capture());
        AlertSignal heatSignal = signalCaptor.getValue();

        assertThat(heatSignal.getSource()).isEqualTo(AlertSignalSource.WEATHER);
        assertThat(heatSignal.getKind()).isEqualTo(AlertSignalKind.HEAT);
        assertThat(heatSignal.getLevel()).isEqualTo(AlertSignalLevel.ALERT);
        assertThat(heatSignal.getScopeType()).isEqualTo(GeographicScopeType.REGION);
        assertThat(heatSignal.getScopeId()).isEqualTo(11L);
        assertThat(heatSignal.getSummary()).contains("Heat alert");
        assertThat(heatSignal.getSummary()).contains("35.0°C");
        assertThat(heatSignal.getSummary()).contains("Île-de-France");
    }

    @Test
    @DisplayName("CRITICAL: Should NOT detect heat alert when temperature = 34.9°C (just below threshold)")
    void shouldNotDetectHeatAlertBelowThreshold() {
        // Given: Weather forecast with max temperature = 34.9°C (just below threshold)
        OpenMeteoForecastResponse.DailyWeather daily = new OpenMeteoForecastResponse.DailyWeather(
            Arrays.asList("2025-11-16"),
            Arrays.asList(34.9),  // 34.9°C - just below 35°C threshold
            null,
            Arrays.asList(50.0),
            Arrays.asList(0.0)
        );
        OpenMeteoForecastResponse forecast = new OpenMeteoForecastResponse("Europe/Paris", daily);

        when(regionRepository.findAll()).thenReturn(Collections.singletonList(ileDeFranceRegion));
        when(openMeteoApiClient.getWeatherForecast(anyDouble(), anyDouble(), anyInt()))
            .thenReturn(Mono.just(forecast));

        // When
        List<AlertSignal> signals = detectionService.detectWeatherSignals();

        // Then: No heat alert should be created
        assertThat(signals).isEmpty();
        verify(alertSignalRepository, never()).save(any());
    }

    @Test
    @DisplayName("CRITICAL: Should detect heat alert when temperature = 40°C (well above threshold)")
    void shouldDetectHeatAlertWellAboveThreshold() {
        // Given: Extreme heat forecast (40°C)
        OpenMeteoForecastResponse.DailyWeather daily = new OpenMeteoForecastResponse.DailyWeather(
            Arrays.asList("2025-11-16"),
            Arrays.asList(40.0),  // 40°C - dangerous heat level
            null,
            Arrays.asList(50.0),
            Arrays.asList(0.0)
        );
        OpenMeteoForecastResponse forecast = new OpenMeteoForecastResponse("Europe/Paris", daily);

        when(regionRepository.findAll()).thenReturn(Collections.singletonList(ileDeFranceRegion));
        when(openMeteoApiClient.getWeatherForecast(anyDouble(), anyDouble(), anyInt()))
            .thenReturn(Mono.just(forecast));
        when(alertSignalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        List<AlertSignal> signals = detectionService.detectWeatherSignals();

        // Then
        assertThat(signals).hasSize(1);
        verify(alertSignalRepository).save(argThat(signal ->
            signal.getKind() == AlertSignalKind.HEAT &&
            signal.getSummary().contains("40.0°C")
        ));
    }

    // ============================================
    // CRITICAL Test 3: Wind Alert Threshold (>= 70 km/h)
    // ============================================

    @Test
    @DisplayName("CRITICAL: Should detect wind alert when speed >= 70.0 km/h")
    void shouldDetectWindAlertAtExactThreshold() {
        // Given: Weather forecast with max wind speed = 70.0 km/h (exact threshold)
        OpenMeteoForecastResponse.DailyWeather daily = new OpenMeteoForecastResponse.DailyWeather(
            Arrays.asList("2025-11-16"),
            Arrays.asList(25.0),
            null,
            Arrays.asList(70.0),  // 70.0 km/h - CRITICAL threshold
            Arrays.asList(0.0)
        );
        OpenMeteoForecastResponse forecast = new OpenMeteoForecastResponse("Europe/Paris", daily);

        when(regionRepository.findAll()).thenReturn(Collections.singletonList(ileDeFranceRegion));
        when(openMeteoApiClient.getWeatherForecast(anyDouble(), anyDouble(), anyInt()))
            .thenReturn(Mono.just(forecast));
        when(alertSignalRepository.save(any())).thenAnswer(inv -> {
            AlertSignal signal = inv.getArgument(0);
            signal.setId(20L);
            return signal;
        });

        // When
        List<AlertSignal> signals = detectionService.detectWeatherSignals();

        // Then
        assertThat(signals).hasSize(1);

        verify(alertSignalRepository).save(signalCaptor.capture());
        AlertSignal windSignal = signalCaptor.getValue();

        assertThat(windSignal.getSource()).isEqualTo(AlertSignalSource.WEATHER);
        assertThat(windSignal.getKind()).isEqualTo(AlertSignalKind.WIND);
        assertThat(windSignal.getLevel()).isEqualTo(AlertSignalLevel.ALERT);
        assertThat(windSignal.getSummary()).contains("Strong wind");
        assertThat(windSignal.getSummary()).contains("70.0 km/h");
    }

    @Test
    @DisplayName("CRITICAL: Should NOT detect wind alert when speed = 69.9 km/h (just below threshold)")
    void shouldNotDetectWindAlertBelowThreshold() {
        // Given: Wind speed just below threshold
        OpenMeteoForecastResponse.DailyWeather daily = new OpenMeteoForecastResponse.DailyWeather(
            Arrays.asList("2025-11-16"),
            Arrays.asList(25.0),
            null,
            Arrays.asList(69.9),  // 69.9 km/h - just below 70 km/h threshold
            Arrays.asList(0.0)
        );
        OpenMeteoForecastResponse forecast = new OpenMeteoForecastResponse("Europe/Paris", daily);

        when(regionRepository.findAll()).thenReturn(Collections.singletonList(ileDeFranceRegion));
        when(openMeteoApiClient.getWeatherForecast(anyDouble(), anyDouble(), anyInt()))
            .thenReturn(Mono.just(forecast));

        // When
        List<AlertSignal> signals = detectionService.detectWeatherSignals();

        // Then
        assertThat(signals).isEmpty();
        verify(alertSignalRepository, never()).save(any());
    }

    // ============================================
    // CRITICAL Test 4: Rain Alert Threshold (>= 30 mm)
    // ============================================

    @Test
    @DisplayName("CRITICAL: Should detect rain alert when precipitation >= 30.0 mm")
    void shouldDetectRainAlertAtExactThreshold() {
        // Given: Weather forecast with precipitation = 30.0 mm (exact threshold)
        OpenMeteoForecastResponse.DailyWeather daily = new OpenMeteoForecastResponse.DailyWeather(
            Arrays.asList("2025-11-16"),
            Arrays.asList(25.0),
            null,
            Arrays.asList(50.0),
            Arrays.asList(30.0)  // 30.0 mm - CRITICAL threshold
        );
        OpenMeteoForecastResponse forecast = new OpenMeteoForecastResponse("Europe/Paris", daily);

        when(regionRepository.findAll()).thenReturn(Collections.singletonList(ileDeFranceRegion));
        when(openMeteoApiClient.getWeatherForecast(anyDouble(), anyDouble(), anyInt()))
            .thenReturn(Mono.just(forecast));
        when(alertSignalRepository.save(any())).thenAnswer(inv -> {
            AlertSignal signal = inv.getArgument(0);
            signal.setId(30L);
            return signal;
        });

        // When
        List<AlertSignal> signals = detectionService.detectWeatherSignals();

        // Then
        assertThat(signals).hasSize(1);

        verify(alertSignalRepository).save(signalCaptor.capture());
        AlertSignal rainSignal = signalCaptor.getValue();

        assertThat(rainSignal.getSource()).isEqualTo(AlertSignalSource.WEATHER);
        assertThat(rainSignal.getKind()).isEqualTo(AlertSignalKind.RAIN);
        assertThat(rainSignal.getLevel()).isEqualTo(AlertSignalLevel.ALERT);
        assertThat(rainSignal.getSummary()).contains("Heavy rain");
        assertThat(rainSignal.getSummary()).contains("30.0 mm");
    }

    @Test
    @DisplayName("CRITICAL: Should NOT detect rain alert when precipitation = 29.9 mm (just below threshold)")
    void shouldNotDetectRainAlertBelowThreshold() {
        // Given: Precipitation just below threshold
        OpenMeteoForecastResponse.DailyWeather daily = new OpenMeteoForecastResponse.DailyWeather(
            Arrays.asList("2025-11-16"),
            Arrays.asList(25.0),
            null,
            Arrays.asList(50.0),
            Arrays.asList(29.9)  // 29.9 mm - just below 30 mm threshold
        );
        OpenMeteoForecastResponse forecast = new OpenMeteoForecastResponse("Europe/Paris", daily);

        when(regionRepository.findAll()).thenReturn(Collections.singletonList(ileDeFranceRegion));
        when(openMeteoApiClient.getWeatherForecast(anyDouble(), anyDouble(), anyInt()))
            .thenReturn(Mono.just(forecast));

        // When
        List<AlertSignal> signals = detectionService.detectWeatherSignals();

        // Then
        assertThat(signals).isEmpty();
        verify(alertSignalRepository, never()).save(any());
    }

    // ============================================
    // Test 5: Multiple Simultaneous Alerts
    // ============================================

    @Test
    @DisplayName("Should detect multiple alert types simultaneously (heat + wind + rain)")
    void shouldDetectMultipleAlertTypesSimultaneously() {
        // Given: Extreme weather with all thresholds exceeded
        OpenMeteoForecastResponse.DailyWeather daily = new OpenMeteoForecastResponse.DailyWeather(
            Arrays.asList("2025-11-16"),
            Arrays.asList(38.0),   // Heat: 38°C > 35°C ✓
            null,
            Arrays.asList(85.0),   // Wind: 85 km/h > 70 km/h ✓
            Arrays.asList(45.0)    // Rain: 45 mm > 30 mm ✓
        );
        OpenMeteoForecastResponse forecast = new OpenMeteoForecastResponse("Europe/Paris", daily);

        when(regionRepository.findAll()).thenReturn(Collections.singletonList(ileDeFranceRegion));
        when(openMeteoApiClient.getWeatherForecast(anyDouble(), anyDouble(), anyInt()))
            .thenReturn(Mono.just(forecast));
        when(alertSignalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        List<AlertSignal> signals = detectionService.detectWeatherSignals();

        // Then: All 3 alert types should be detected
        assertThat(signals).hasSize(3);

        verify(alertSignalRepository, times(3)).save(signalCaptor.capture());
        List<AlertSignal> capturedSignals = signalCaptor.getAllValues();

        assertThat(capturedSignals).extracting(AlertSignal::getKind)
            .containsExactlyInAnyOrder(
                AlertSignalKind.HEAT,
                AlertSignalKind.WIND,
                AlertSignalKind.RAIN
            );
    }

    // ============================================
    // Test 6: Error Handling
    // ============================================

    @Test
    @DisplayName("Should handle ATMO API failure gracefully")
    void shouldHandleAtmoApiFailureGracefully() {
        // Given: ATMO API throws exception
        when(atmoApiClient.getCurrentAirQualityIndices())
            .thenReturn(Flux.error(new RuntimeException("ATMO API unavailable")));

        // When
        List<AlertSignal> signals = detectionService.detectAtmoSignals();

        // Then: Should return empty list, not throw exception
        assertThat(signals).isEmpty();
        verify(alertSignalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle Weather API failure gracefully")
    void shouldHandleWeatherApiFailureGracefully() {
        // Given: Weather API throws exception
        when(regionRepository.findAll()).thenReturn(Collections.singletonList(ileDeFranceRegion));
        when(openMeteoApiClient.getWeatherForecast(anyDouble(), anyDouble(), anyInt()))
            .thenReturn(Mono.error(new RuntimeException("Weather API unavailable")));

        // When
        List<AlertSignal> signals = detectionService.detectWeatherSignals();

        // Then: Should return empty list, not throw exception
        assertThat(signals).isEmpty();
        verify(alertSignalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle empty ATMO data gracefully")
    void shouldHandleEmptyAtmoDataGracefully() {
        // Given: No ATMO data available
        when(atmoApiClient.getCurrentAirQualityIndices())
            .thenReturn(Flux.empty());

        // When
        List<AlertSignal> signals = detectionService.detectAtmoSignals();

        // Then
        assertThat(signals).isEmpty();
        verify(alertSignalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle no regions found gracefully")
    void shouldHandleNoRegionsFoundGracefully() {
        // Given: No regions in database
        when(regionRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<AlertSignal> signals = detectionService.detectWeatherSignals();

        // Then
        assertThat(signals).isEmpty();
        verify(openMeteoApiClient, never()).getWeatherForecast(anyDouble(), anyDouble(), anyInt());
    }

    // ============================================
    // Test 7: Manual Signal Creation
    // ============================================

    @Test
    @DisplayName("Should create manual alert signal with admin input")
    void shouldCreateManualAlertSignal() {
        // Given
        CreateManualSignalRequest request = new CreateManualSignalRequest(
            AlertSignalKind.HEAT,
            AlertSignalLevel.WARNING,
            GeographicScopeType.DEPARTMENT,
            75L,
            "Manual heat alert for Paris",
            "Admin-created signal for testing purposes",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(2)
        );

        when(alertSignalRepository.save(any())).thenAnswer(inv -> {
            AlertSignal signal = inv.getArgument(0);
            signal.setId(100L);
            return signal;
        });

        // When
        AlertSignal result = detectionService.createManualSignal(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);

        verify(alertSignalRepository).save(signalCaptor.capture());
        AlertSignal capturedSignal = signalCaptor.getValue();

        assertThat(capturedSignal.getSource()).isEqualTo(AlertSignalSource.WEATHER);
        assertThat(capturedSignal.getKind()).isEqualTo(AlertSignalKind.HEAT);
        assertThat(capturedSignal.getLevel()).isEqualTo(AlertSignalLevel.WARNING);
        assertThat(capturedSignal.getScopeType()).isEqualTo(GeographicScopeType.DEPARTMENT);
        assertThat(capturedSignal.getScopeId()).isEqualTo(75L);
        assertThat(capturedSignal.getSummary()).isEqualTo("Manual heat alert for Paris");
        assertThat(capturedSignal.getDetails()).isEqualTo("Admin-created signal for testing purposes");
    }

    // ============================================
    // Test 8: Null Safety
    // ============================================

    @Test
    @DisplayName("Should handle null temperature values in forecast")
    void shouldHandleNullTemperatureValues() {
        // Given: Forecast with null temperature values
        OpenMeteoForecastResponse.DailyWeather daily = new OpenMeteoForecastResponse.DailyWeather(
            Arrays.asList("2025-11-16"),
            Arrays.asList((Double) null),  // Null temperature
            null,
            Arrays.asList(50.0),
            Arrays.asList(10.0)
        );
        OpenMeteoForecastResponse forecast = new OpenMeteoForecastResponse("Europe/Paris", daily);

        when(regionRepository.findAll()).thenReturn(Collections.singletonList(ileDeFranceRegion));
        when(openMeteoApiClient.getWeatherForecast(anyDouble(), anyDouble(), anyInt()))
            .thenReturn(Mono.just(forecast));

        // When
        List<AlertSignal> signals = detectionService.detectWeatherSignals();

        // Then: Should not crash, and should not detect heat alert
        assertThat(signals).isEmpty();
    }

    @Test
    @DisplayName("Should handle null ATMO index gracefully")
    void shouldHandleNullAtmoIndexGracefully() {
        // Given: ATMO data with null index
        AtmoAirQualityResponse nullIndexData = new AtmoAirQualityResponse(
            "75056",
            "Paris",
            "2025-11-16",
            null,  // Null ATMO index
            "Unknown",
            "#000000",
            null, null, null, null, null
        );

        when(atmoApiClient.getCurrentAirQualityIndices())
            .thenReturn(Flux.just(nullIndexData));

        // When
        List<AlertSignal> signals = detectionService.detectAtmoSignals();

        // Then: Should not crash or create alert
        assertThat(signals).isEmpty();
        verify(alertSignalRepository, never()).save(any());
    }
}
