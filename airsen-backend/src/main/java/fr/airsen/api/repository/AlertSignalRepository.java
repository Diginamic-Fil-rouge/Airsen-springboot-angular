package fr.airsen.api.repository;

import fr.airsen.api.entity.AlertSignal;
import fr.airsen.api.entity.enums.AlertSignalLevel;
import fr.airsen.api.entity.enums.AlertSignalSource;
import fr.airsen.api.entity.enums.GeographicScopeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertSignalRepository extends JpaRepository<AlertSignal, Long> {

    /**
     * Finds alert signals by source and level with pagination.
     * Used for filtering admin dashboard by specific criteria.
     *
     * @param source alert signal source (ATMO or WEATHER)
     * @param level alert severity level (INFO, WATCH, ALERT)
     * @param pageable pagination parameters
     * @return page of matching alert signals
     */
    Page<AlertSignal> findBySourceAndLevel(AlertSignalSource source, AlertSignalLevel level, Pageable pageable);

    /**
     * Finds alert signals by geographic scope.
     * Used for displaying alerts for specific regions, departments, or communes.
     *
     * @param scopeType geographic scope type (FRANCE, REGION, DEPARTMENT, COMMUNE)
     * @param scopeId identifier of the geographic entity (null for FRANCE)
     * @return list of matching alert signals
     */
    List<AlertSignal> findByScopeTypeAndScopeId(GeographicScopeType scopeType, Long scopeId);

    /**
     * Finds alert signals detected within a date range.
     * Used for historical analysis and reporting.
     *
     * @param start start of date range
     * @param end end of date range
     * @return list of alert signals detected in the range
     */
    List<AlertSignal> findAllByDetectedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Finds active alert signals by level.
     * Active alerts are those with validTo date in the future.
     * Used for displaying current alerts on admin dashboard.
     *
     * @param level alert severity level (INFO, WATCH, ALERT)
     * @param now current date/time to compare against validTo
     * @return list of active alert signals
     */
    List<AlertSignal> findByLevelAndValidToAfter(AlertSignalLevel level, LocalDateTime now);

    /**
     * Counts alert signals by source detected after a specific date.
     * Used for statistics and analytics.
     *
     * @param source alert signal source (ATMO or WEATHER)
     * @param since date from which to count
     * @return count of alert signals from the source
     */
    Long countBySourceAndDetectedAtAfter(AlertSignalSource source, LocalDateTime since);

    /**
     * Finds all currently active alert signals across all scopes.
     * Active alerts have validFrom in the past (or null) and validTo in the future (or null).
     *
     * @param now current date/time for comparison
     * @return list of active alert signals
     */
    @Query("SELECT a FROM AlertSignal a WHERE " +
           "(a.validFrom IS NULL OR a.validFrom <= :now) AND " +
           "(a.validTo IS NULL OR a.validTo > :now) " +
           "ORDER BY a.level DESC, a.detectedAt DESC")
    List<AlertSignal> findAllActiveAlerts(@Param("now") LocalDateTime now);

    /**
     * Finds all currently active alert signals across all scopes with pagination.
     *
     * @param now current date/time for comparison
     * @param pageable pagination parameters
     * @return page of active alert signals
     */
    @Query("SELECT a FROM AlertSignal a WHERE " +
           "(a.validFrom IS NULL OR a.validFrom <= :now) AND " +
           "(a.validTo IS NULL OR a.validTo > :now) " +
           "ORDER BY a.level DESC, a.detectedAt DESC")
    Page<AlertSignal> findAllActiveAlerts(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Finds alert signals detected in the last 24 hours.
     * Used for recent alerts dashboard widget.
     *
     * @param since date/time 24 hours ago
     * @return list of recent alert signals
     */
    @Query("SELECT a FROM AlertSignal a WHERE a.detectedAt >= :since ORDER BY a.detectedAt DESC")
    List<AlertSignal> findRecentAlerts(@Param("since") LocalDateTime since);

    /**
     * Finds alert signals by multiple filters with pagination.
     * All parameters are optional (null means no filter applied).
     *
     * @param source alert signal source (ATMO or WEATHER), nullable
     * @param level alert severity level (INFO, WATCH, ALERT), nullable
     * @param scopeType geographic scope type, nullable
     * @param pageable pagination parameters
     * @return page of matching alert signals
     */
    @Query("SELECT a FROM AlertSignal a WHERE " +
           "(:source IS NULL OR a.source = :source) AND " +
           "(:level IS NULL OR a.level = :level) AND " +
           "(:scopeType IS NULL OR a.scopeType = :scopeType) " +
           "ORDER BY a.detectedAt DESC")
    Page<AlertSignal> findByMultipleFilters(
        @Param("source") AlertSignalSource source,
        @Param("level") AlertSignalLevel level,
        @Param("scopeType") GeographicScopeType scopeType,
        Pageable pageable
    );

    /**
     * Finds alert signals by scope type with pagination.
     * Used for filtering alerts by geographic scope.
     *
     * @param scopeType geographic scope type (FRANCE, REGION, DEPARTMENT, COMMUNE)
     * @param pageable pagination parameters
     * @return page of matching alert signals
     */
    Page<AlertSignal> findByScopeType(GeographicScopeType scopeType, Pageable pageable);

    /**
     * Finds alert signals by source with pagination.
     * Used for filtering alerts by data source.
     *
     * @param source alert signal source (ATMO or WEATHER)
     * @param pageable pagination parameters
     * @return page of matching alert signals
     */
    Page<AlertSignal> findBySource(AlertSignalSource source, Pageable pageable);

    /**
     * Finds alert signals by level with pagination.
     * Used for filtering alerts by severity level.
     *
     * @param level alert severity level (INFO, WATCH, ALERT)
     * @param pageable pagination parameters
     * @return page of matching alert signals
     */
    Page<AlertSignal> findByLevel(AlertSignalLevel level, Pageable pageable);

    /**
     * Finds active alerts for a specific geographic scope.
     *
     * @param scopeType geographic scope type
     * @param scopeId identifier of the geographic entity
     * @param now current date/time for comparison
     * @return list of active alert signals for the scope
     */
    @Query("SELECT a FROM AlertSignal a WHERE " +
           "a.scopeType = :scopeType AND " +
           "a.scopeId = :scopeId AND " +
           "(a.validFrom IS NULL OR a.validFrom <= :now) AND " +
           "(a.validTo IS NULL OR a.validTo > :now) " +
           "ORDER BY a.level DESC, a.detectedAt DESC")
    List<AlertSignal> findActiveByScopeTypeAndScopeId(
        @Param("scopeType") GeographicScopeType scopeType,
        @Param("scopeId") Long scopeId,
        @Param("now") LocalDateTime now
    );

    /**
     * Counts active alert signals by level.
     * Used for dashboard statistics.
     *
     * @param level alert severity level
     * @param now current date/time for comparison
     * @return count of active alerts at the specified level
     */
    @Query("SELECT COUNT(a) FROM AlertSignal a WHERE " +
           "a.level = :level AND " +
           "(a.validFrom IS NULL OR a.validFrom <= :now) AND " +
           "(a.validTo IS NULL OR a.validTo > :now)")
    Long countActiveByLevel(@Param("level") AlertSignalLevel level, @Param("now") LocalDateTime now);

    /**
     * Counts alert signals by source and level detected after a specific date.
     * Used for detailed statistics.
     *
     * @param source alert signal source
     * @param level alert severity level
     * @param since date from which to count
     * @return count of matching alert signals
     */
    Long countBySourceAndLevelAndDetectedAtAfter(AlertSignalSource source, AlertSignalLevel level, LocalDateTime since);

    /**
     * Finds expired alert signals that should be archived.
     * Expired alerts have validTo date in the past.
     *
     * @param now current date/time for comparison
     * @return list of expired alert signals
     */
    @Query("SELECT a FROM AlertSignal a WHERE a.validTo IS NOT NULL AND a.validTo < :now ORDER BY a.validTo DESC")
    List<AlertSignal> findExpiredAlerts(@Param("now") LocalDateTime now);

    /**
     * Finds alert signals by source and date range with pagination.
     *
     * @param source alert signal source
     * @param start start of date range
     * @param end end of date range
     * @param pageable pagination parameters
     * @return page of matching alert signals
     */
    @Query("SELECT a FROM AlertSignal a WHERE " +
           "a.source = :source AND " +
           "a.detectedAt BETWEEN :start AND :end " +
           "ORDER BY a.detectedAt DESC")
    Page<AlertSignal> findBySourceAndDetectedAtBetween(
        @Param("source") AlertSignalSource source,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
    );

    /**
     * Finds alert signal statistics by source grouped by level.
     * Returns aggregated data for reporting.
     *
     * @param source alert signal source
     * @param since date from which to gather statistics
     * @return list of statistics [level, count]
     */
    @Query("SELECT a.level, COUNT(a) FROM AlertSignal a WHERE " +
           "a.source = :source AND a.detectedAt >= :since " +
           "GROUP BY a.level ORDER BY a.level DESC")
    List<Object[]> findStatsBySourceSince(@Param("source") AlertSignalSource source, @Param("since") LocalDateTime since);

    /**
     * Finds alert signal statistics by scope type.
     *
     * @param since date from which to gather statistics
     * @return list of statistics [scopeType, count]
     */
    @Query("SELECT a.scopeType, COUNT(a) FROM AlertSignal a WHERE " +
           "a.detectedAt >= :since " +
           "GROUP BY a.scopeType ORDER BY COUNT(a) DESC")
    List<Object[]> findStatsByScopeTypeSince(@Param("since") LocalDateTime since);

    /**
     * Checks if there are any active ALERT level signals for a specific scope.
     * Used for emergency notifications.
     *
     * @param scopeType geographic scope type
     * @param scopeId identifier of the geographic entity
     * @param now current date/time for comparison
     * @return true if critical alerts exist for the scope
     */
    @Query("SELECT COUNT(a) > 0 FROM AlertSignal a WHERE " +
           "a.level = 'ALERT' AND " +
           "a.scopeType = :scopeType AND " +
           "a.scopeId = :scopeId AND " +
           "(a.validFrom IS NULL OR a.validFrom <= :now) AND " +
           "(a.validTo IS NULL OR a.validTo > :now)")
    boolean hasCriticalAlertsForScope(
        @Param("scopeType") GeographicScopeType scopeType,
        @Param("scopeId") Long scopeId,
        @Param("now") LocalDateTime now
    );

    /**
     * Finds the most recent alert signal for a specific scope and source.
     * Used to avoid duplicate signal detection.
     *
     * @param source alert signal source
     * @param scopeType geographic scope type
     * @param scopeId identifier of the geographic entity
     * @return the most recent alert signal, or null if none exists
     */
    @Query("SELECT a FROM AlertSignal a WHERE " +
           "a.source = :source AND " +
           "a.scopeType = :scopeType AND " +
           "(:scopeId IS NULL OR a.scopeId = :scopeId) " +
           "ORDER BY a.detectedAt DESC LIMIT 1")
    AlertSignal findMostRecentByScopeAndSource(
        @Param("source") AlertSignalSource source,
        @Param("scopeType") GeographicScopeType scopeType,
        @Param("scopeId") Long scopeId
    );
}
