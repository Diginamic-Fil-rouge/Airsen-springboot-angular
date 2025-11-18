package fr.airsen.api.controller;

import fr.airsen.api.dto.request.CreateManualSignalRequest;
import fr.airsen.api.dto.response.AlertSignalDTO;
import fr.airsen.api.entity.AlertSignal;
import fr.airsen.api.entity.enums.*;
import fr.airsen.api.mapper.AlertSignalMapper;
import fr.airsen.api.service.AlertSignalDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminAlertSignalController.
 *
 * SECURITY CRITICAL:
 * - All endpoints require ADMIN role (@PreAuthorize("hasRole('ADMIN')"))
 * - Tests verify proper authorization checks
 * - Tests verify admin-only operations (create, delete, statistics)
 *
 * NOTE: @PreAuthorize annotation is tested via Spring Security integration tests.
 * These unit tests focus on controller logic assuming authorization passed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAlertSignalController Unit Tests")
class AdminAlertSignalControllerTest {

    @Mock
    private AlertSignalDetectionService detectionService;

    @Mock
    private AlertSignalMapper alertSignalMapper;

    @InjectMocks
    private AdminAlertSignalController controller;

    private AlertSignal testSignal;
    private AlertSignalDTO testSignalDTO;

    @BeforeEach
    void setUp() {
        setupTestEntities();
    }

    private void setupTestEntities() {
        // Test Alert Signal entity
        testSignal = new AlertSignal(
            AlertSignalSource.ATMO,
            AlertSignalKind.AQI,
            AlertSignalLevel.ALERT,
            GeographicScopeType.COMMUNE,
            1L,
            "High air pollution detected",
            LocalDateTime.now()
        );
        testSignal.setId(1L);
        testSignal.setDetails("PM2.5 levels exceed safe thresholds");
        testSignal.setValidFrom(LocalDateTime.now());
        testSignal.setValidTo(LocalDateTime.now().plusDays(1));

        // Test Alert Signal DTO
        testSignalDTO = new AlertSignalDTO(
            1L,
            AlertSignalSource.ATMO,
            AlertSignalKind.AQI,
            AlertSignalLevel.ALERT,
            GeographicScopeType.COMMUNE,
            1L,
            "High air pollution detected",
            "PM2.5 levels exceed safe thresholds",
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    // ============================================
    // Test 1: GET /admin/alert-signals - List All Signals
    // ============================================

    @Test
    @DisplayName("Should retrieve all alert signals with pagination")
    void shouldRetrieveAllAlertSignalsWithPagination() {
        // Given
        AlertSignal signal1 = createTestSignal(1L, AlertSignalKind.AQI, "Air quality alert");
        AlertSignal signal2 = createTestSignal(2L, AlertSignalKind.HEAT, "Heat wave warning");

        Page<AlertSignal> signalPage = new PageImpl<>(
            Arrays.asList(signal1, signal2),
            PageRequest.of(0, 10),
            2
        );

        AlertSignalDTO dto1 = createTestDTO(1L, AlertSignalKind.AQI, "Air quality alert");
        AlertSignalDTO dto2 = createTestDTO(2L, AlertSignalKind.HEAT, "Heat wave warning");

        when(detectionService.getAllSignals(any(Pageable.class)))
            .thenReturn(signalPage);
        when(alertSignalMapper.toDTO(signal1)).thenReturn(dto1);
        when(alertSignalMapper.toDTO(signal2)).thenReturn(dto2);

        // When
        ResponseEntity<Page<AlertSignalDTO>> response = controller.getAllSignals(
            null, null, null, PageRequest.of(0, 10)
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(2);
        assertThat(response.getBody().getTotalElements()).isEqualTo(2);

        verify(detectionService).getAllSignals(any(Pageable.class));
        verify(alertSignalMapper, times(2)).toDTO(any());
    }

    @Test
    @DisplayName("Should return empty page when no signals exist")
    void shouldReturnEmptyPageWhenNoSignalsExist() {
        // Given
        Page<AlertSignal> emptyPage = new PageImpl<>(
            Collections.emptyList(),
            PageRequest.of(0, 10),
            0
        );

        when(detectionService.getAllSignals(any(Pageable.class)))
            .thenReturn(emptyPage);

        // When
        ResponseEntity<Page<AlertSignalDTO>> response = controller.getAllSignals(
            null, null, null, PageRequest.of(0, 10)
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
        assertThat(response.getBody().getTotalElements()).isZero();
    }

    // ============================================
    // Test 2: GET /admin/alert-signals/{id} - Get Signal By ID
    // ============================================

    @Test
    @DisplayName("Should retrieve alert signal by ID")
    void shouldRetrieveAlertSignalById() {
        // Given
        Long signalId = 1L;
        when(detectionService.getSignalById(signalId))
            .thenReturn(Optional.of(testSignal));
        when(alertSignalMapper.toDTO(testSignal))
            .thenReturn(testSignalDTO);

        // When
        ResponseEntity<AlertSignalDTO> response = controller.getSignalById(signalId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(1L);
        assertThat(response.getBody().summary()).isEqualTo("High air pollution detected");

        verify(detectionService).getSignalById(signalId);
        verify(alertSignalMapper).toDTO(testSignal);
    }

    @Test
    @DisplayName("Should return 404 when signal not found by ID")
    void shouldReturn404WhenSignalNotFoundById() {
        // Given
        Long invalidId = 999L;
        when(detectionService.getSignalById(invalidId))
            .thenReturn(Optional.empty());

        // When
        ResponseEntity<AlertSignalDTO> response = controller.getSignalById(invalidId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();

        verify(detectionService).getSignalById(invalidId);
        verify(alertSignalMapper, never()).toDTO(any());
    }

    // ============================================
    // Test 3: POST /admin/alert-signals/manual - Create Manual Signal
    // ============================================

    @Test
    @DisplayName("Should create manual alert signal successfully")
    void shouldCreateManualAlertSignalSuccessfully() {
        // Given
        CreateManualSignalRequest request = new CreateManualSignalRequest(
            AlertSignalSource.WEATHER,
            AlertSignalKind.HEAT,
            AlertSignalLevel.ALERT,
            GeographicScopeType.DEPARTMENT,
            75L,
            "Manual heat alert for Paris",
            "Admin-created signal for testing",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(2)
        );

        AlertSignal createdSignal = new AlertSignal(
            AlertSignalSource.WEATHER,
            AlertSignalKind.HEAT,
            AlertSignalLevel.ALERT,
            GeographicScopeType.DEPARTMENT,
            75L,
            "Manual heat alert for Paris",
            LocalDateTime.now()
        );
        createdSignal.setId(100L);

        AlertSignalDTO createdDTO = new AlertSignalDTO(
            100L,
            AlertSignalSource.WEATHER,
            AlertSignalKind.HEAT,
            AlertSignalLevel.ALERT,
            GeographicScopeType.DEPARTMENT,
            75L,
            "Manual heat alert for Paris",
            "Admin-created signal for testing",
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(2),
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        when(detectionService.createManualSignal(request))
            .thenReturn(createdSignal);
        when(alertSignalMapper.toDTO(createdSignal))
            .thenReturn(createdDTO);

        // When
        ResponseEntity<AlertSignalDTO> response = controller.createManualSignal(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(100L);
        assertThat(response.getBody().summary()).isEqualTo("Manual heat alert for Paris");
        assertThat(response.getBody().kind()).isEqualTo(AlertSignalKind.HEAT);
        assertThat(response.getBody().level()).isEqualTo(AlertSignalLevel.ALERT);

        verify(detectionService).createManualSignal(request);
        verify(alertSignalMapper).toDTO(createdSignal);
    }

    @Test
    @DisplayName("Should create manual signal for all geographic scope types")
    void shouldCreateManualSignalForAllScopeTypes() {
        // Test FRANCE scope
        CreateManualSignalRequest franceRequest = new CreateManualSignalRequest(
            AlertSignalSource.WEATHER,
            AlertSignalKind.WIND,
            AlertSignalLevel.INFO,
            GeographicScopeType.FRANCE,
            null,
            "National wind advisory",
            "Details",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1)
        );

        AlertSignal franceSignal = new AlertSignal(
            AlertSignalSource.WEATHER,
            AlertSignalKind.WIND,
            AlertSignalLevel.INFO,
            GeographicScopeType.FRANCE,
            null,
            "National wind advisory",
            LocalDateTime.now()
        );
        franceSignal.setId(200L);

        when(detectionService.createManualSignal(franceRequest))
            .thenReturn(franceSignal);
        when(alertSignalMapper.toDTO(any()))
            .thenReturn(createTestDTO(200L, AlertSignalKind.WIND, "National wind advisory"));

        ResponseEntity<AlertSignalDTO> response = controller.createManualSignal(franceRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().scopeType()).isEqualTo(GeographicScopeType.FRANCE);
        assertThat(response.getBody().scopeId()).isNull();
    }

    // ============================================
    // Test 4: POST /admin/alert-signals/refresh - Trigger Detection
    // ============================================

    @Test
    @DisplayName("Should trigger signal detection successfully")
    void shouldTriggerSignalDetectionSuccessfully() {
        // Given
        doNothing().when(detectionService).detectAllSignals();

        // When
        ResponseEntity<Map<String, Object>> response = controller.refreshSignals();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("success");
        assertThat(response.getBody().get("message")).isEqualTo("Signal detection completed successfully");
        assertThat(response.getBody()).containsKey("timestamp");

        verify(detectionService).detectAllSignals();
    }

    @Test
    @DisplayName("Should return 500 when detection fails")
    void shouldReturn500WhenDetectionFails() {
        // Given
        doThrow(new RuntimeException("External API unavailable"))
            .when(detectionService).detectAllSignals();

        // When
        ResponseEntity<Map<String, Object>> response = controller.refreshSignals();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("error");
        assertThat(response.getBody().get("message")).asString()
            .contains("Signal detection failed")
            .contains("External API unavailable");
        assertThat(response.getBody()).containsKey("timestamp");

        verify(detectionService).detectAllSignals();
    }

    // ============================================
    // Test 5: DELETE /admin/alert-signals/{id} - Delete Signal
    // ============================================

    @Test
    @DisplayName("Should delete alert signal successfully")
    void shouldDeleteAlertSignalSuccessfully() {
        // Given
        Long signalId = 1L;
        doNothing().when(detectionService).deleteSignal(signalId);

        // When
        ResponseEntity<Void> response = controller.deleteSignal(signalId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        verify(detectionService).deleteSignal(signalId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent signal")
    void shouldThrowExceptionWhenDeletingNonExistentSignal() {
        // Given
        Long invalidId = 999L;
        doThrow(new IllegalArgumentException("Alert signal not found with ID: 999"))
            .when(detectionService).deleteSignal(invalidId);

        // When & Then
        assertThatThrownBy(() -> controller.deleteSignal(invalidId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Alert signal not found");

        verify(detectionService).deleteSignal(invalidId);
    }

    // ============================================
    // Test 6: GET /admin/alert-signals/statistics - Get Statistics
    // ============================================

    @Test
    @DisplayName("Should calculate and return signal statistics")
    void shouldCalculateAndReturnSignalStatistics() {
        // Given
        AlertSignal atmoSignal1 = createTestSignal(1L, AlertSignalKind.AQI, "AQI alert 1");
        atmoSignal1.setSource(AlertSignalSource.ATMO);
        atmoSignal1.setLevel(AlertSignalLevel.ALERT);
        atmoSignal1.setDetectedAt(LocalDateTime.now().minusHours(2));

        AlertSignal atmoSignal2 = createTestSignal(2L, AlertSignalKind.AQI, "AQI alert 2");
        atmoSignal2.setSource(AlertSignalSource.ATMO);
        atmoSignal2.setLevel(AlertSignalLevel.WATCH);
        atmoSignal2.setDetectedAt(LocalDateTime.now().minusHours(1));

        AlertSignal weatherSignal = createTestSignal(3L, AlertSignalKind.HEAT, "Heat alert");
        weatherSignal.setSource(AlertSignalSource.WEATHER);
        weatherSignal.setLevel(AlertSignalLevel.ALERT);
        weatherSignal.setDetectedAt(LocalDateTime.now());  // Most recent

        Page<AlertSignal> signalsPage = new PageImpl<>(
            Arrays.asList(atmoSignal1, atmoSignal2, weatherSignal)
        );

        when(detectionService.getAllSignals(Pageable.unpaged()))
            .thenReturn(signalsPage);

        // When
        ResponseEntity<fr.airsen.api.dto.response.AlertSignalStatisticsDTO> response =
            controller.getStatistics();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        // Verify total count
        assertThat(response.getBody().totalSignals()).isEqualTo(3);

        // Verify source breakdown
        Map<String, Integer> bySource = response.getBody().bySource();
        assertThat(bySource.get("ATMO")).isEqualTo(2);
        assertThat(bySource.get("WEATHER")).isEqualTo(1);

        // Verify level breakdown
        Map<String, Integer> byLevel = response.getBody().byLevel();
        assertThat(byLevel.get("ALERT")).isEqualTo(2);
        assertThat(byLevel.get("WATCH")).isEqualTo(1);

        // Verify last detection timestamp (should be weatherSignal's timestamp)
        assertThat(response.getBody().lastDetectionAt()).isNotNull();
        assertThat(response.getBody().lastDetectionAt())
            .isCloseTo(weatherSignal.getDetectedAt(), within(1, java.time.temporal.ChronoUnit.SECONDS));

        verify(detectionService).getAllSignals(Pageable.unpaged());
    }

    @Test
    @DisplayName("Should return empty statistics when no signals exist")
    void shouldReturnEmptyStatisticsWhenNoSignalsExist() {
        // Given
        Page<AlertSignal> emptyPage = new PageImpl<>(Collections.emptyList());
        when(detectionService.getAllSignals(Pageable.unpaged()))
            .thenReturn(emptyPage);

        // When
        ResponseEntity<fr.airsen.api.dto.response.AlertSignalStatisticsDTO> response =
            controller.getStatistics();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalSignals()).isZero();
        assertThat(response.getBody().bySource()).isEmpty();
        assertThat(response.getBody().byLevel()).isEmpty();
        assertThat(response.getBody().lastDetectionAt()).isNull();
    }

    @Test
    @DisplayName("Should calculate statistics with multiple signal types")
    void shouldCalculateStatisticsWithMultipleSignalTypes() {
        // Given: Mix of different signal types
        AlertSignal heatAlert = createTestSignal(1L, AlertSignalKind.HEAT, "Heat");
        heatAlert.setSource(AlertSignalSource.WEATHER);
        heatAlert.setLevel(AlertSignalLevel.ALERT);

        AlertSignal windWarning = createTestSignal(2L, AlertSignalKind.WIND, "Wind");
        windWarning.setSource(AlertSignalSource.WEATHER);
        windWarning.setLevel(AlertSignalLevel.WATCH);

        AlertSignal rainInfo = createTestSignal(3L, AlertSignalKind.RAIN, "Rain");
        rainInfo.setSource(AlertSignalSource.WEATHER);
        rainInfo.setLevel(AlertSignalLevel.INFO);

        AlertSignal aqiAlert = createTestSignal(4L, AlertSignalKind.AQI, "AQI");
        aqiAlert.setSource(AlertSignalSource.ATMO);
        aqiAlert.setLevel(AlertSignalLevel.ALERT);

        Page<AlertSignal> signalsPage = new PageImpl<>(
            Arrays.asList(heatAlert, windWarning, rainInfo, aqiAlert)
        );

        when(detectionService.getAllSignals(Pageable.unpaged()))
            .thenReturn(signalsPage);

        // When
        ResponseEntity<fr.airsen.api.dto.response.AlertSignalStatisticsDTO> response =
            controller.getStatistics();

        // Then
        assertThat(response.getBody().totalSignals()).isEqualTo(4);
        assertThat(response.getBody().bySource().get("WEATHER")).isEqualTo(3);
        assertThat(response.getBody().bySource().get("ATMO")).isEqualTo(1);
        assertThat(response.getBody().byLevel().get("ALERT")).isEqualTo(2);
        assertThat(response.getBody().byLevel().get("WATCH")).isEqualTo(1);
        assertThat(response.getBody().byLevel().get("INFO")).isEqualTo(1);
    }

    // ============================================
    // Helper Methods
    // ============================================

    private AlertSignal createTestSignal(Long id, AlertSignalKind kind, String summary) {
        AlertSignal signal = new AlertSignal(
            AlertSignalSource.ATMO,
            kind,
            AlertSignalLevel.ALERT,
            GeographicScopeType.COMMUNE,
            1L,
            summary,
            LocalDateTime.now()
        );
        signal.setId(id);
        signal.setDetectedAt(LocalDateTime.now());
        return signal;
    }

    private AlertSignalDTO createTestDTO(Long id, AlertSignalKind kind, String summary) {
        return new AlertSignalDTO(
            id,
            AlertSignalSource.ATMO,
            kind,
            AlertSignalLevel.ALERT,
            GeographicScopeType.COMMUNE,
            1L,
            summary,
            "Details",
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
}
