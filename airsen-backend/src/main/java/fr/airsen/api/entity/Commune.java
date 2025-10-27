package fr.airsen.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a French commune.
 *
 * Lowest administrative level of the French geographic hierarchy.
 * Contains demographic and geographic data for interactive map display
 * and proximity search.
 */
@Entity
@Table(name = "communes")
public class Commune {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "insee_code", unique = true, nullable = false, length = 10)
    @NotBlank(message = "INSEE code must not be empty")
    @Size(min = 5, max = 5, message = "INSEE code must follow official 5-digit format")
    private String inseeCode;

    @Column(name = "name", nullable = false, length = 255)
    @NotBlank(message = "Commune name must not be empty")
    @Size(max = 255, message = "Commune name must not exceed 255 characters")
    private String name;

    @Column(name = "department_code", length = 10)
    @Size(max = 10, message = "Department code must not exceed 10 characters")
    private String departmentCode;

    @Column(name = "region_code", length = 10)
    @Size(max = 10, message = "Region code must not exceed 10 characters")
    private String regionCode;

    @Column(name = "population", nullable = false)
    @PositiveOrZero(message = "Population must be non-negative")
    private long population;

    @Column(name = "latitude", precision = 10, scale = 8)
    @DecimalMin(value = "41.0", message = "Latitude must be within French territory bounds")
    @DecimalMax(value = "51.0", message = "Latitude must be within French territory bounds")
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    @DecimalMin(value = "-5.0", message = "Longitude must be within French territory bounds")
    @DecimalMax(value = "10.0", message = "Longitude must be within French territory bounds")
    private BigDecimal longitude;

    @OneToMany(mappedBy = "commune", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<WeatherData> weatherData = new HashSet<>();

    @OneToMany(mappedBy = "commune", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<AirQuality> airQuality = new HashSet<>();

    /**
     * Users who favorited this commune.
     *
     * Bidirectional relationship through UserFavorite join entity.
     * Useful for analytics queries (e.g., "most popular communes").
     * Lazy loading prevents performance issues.
     */
    @OneToMany(mappedBy = "commune", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<UserFavorite> favoritedBy = new HashSet<>();

    /**
     * Air quality alerts monitoring this commune.
     */
//    @OneToMany(mappedBy = "commune", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @JsonIgnore
//    private Set<Alert> alerts = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    @NotNull(message = "Commune must belong to a valid department")
    @JsonIgnore
    private Department department;

    public Commune() {}

    public Commune(String inseeCode, String name, String departmentCode, String regionCode,
                   long population, BigDecimal latitude, BigDecimal longitude, Department department) {
        this.inseeCode = inseeCode;
        this.name = name;
        this.departmentCode = departmentCode;
        this.regionCode = regionCode;
        this.population = population;
        this.latitude = latitude;
        this.longitude = longitude;
        this.department = department;
    }

    // Getters & Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getInseeCode() {
        return inseeCode;
    }

    public void setInseeCode(String inseeCode) {
        this.inseeCode = inseeCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public long getPopulation() {
        return population;
    }

    public void setPopulation(long population) {
        this.population = population;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Set<WeatherData> getWeatherData() {
        return weatherData;
    }

    public void setWeatherData(Set<WeatherData> weatherData) {
        this.weatherData = weatherData;
    }

    public Set<AirQuality> getAirQuality() {
        return airQuality;
    }

    public void setAirQuality(Set<AirQuality> airQuality) {
        this.airQuality = airQuality;
    }

    /**
     * Gets users who favorited this commune.
     *
     * @return Set of UserFavorite entities
     */
    public Set<UserFavorite> getFavoritedBy() {
        return favoritedBy;
    }

    public void setFavoritedBy(Set<UserFavorite> favoritedBy) {
        this.favoritedBy = favoritedBy;
    }

    /**
     * Gets the number of users who favorited this commune.
     *
     * Useful for analytics and "popular communes" features.
     * This method should only be called within an active Hibernate session.
     * For JSON serialization, use a dedicated DTO to avoid lazy initialization issues.
     *
     * @return Count of favorites, or 0 if collection is not initialized
     */
    @JsonIgnore
    public int getFavoriteCount() {
        return favoritedBy != null ? favoritedBy.size() : 0;
    }
//
//    public Set<Alert> getAlerts() {
//        return alerts;
//    }
//
//    public void setAlerts(Set<Alert> alerts) {
//        this.alerts = alerts;
//    }

    // External API integration methods
    public void setArea(Double area) {
        // Area data can be added as a field if needed in future
        // For now, this is a placeholder for INSEE API compatibility
    }

    public Double getArea() {
        // Return a default area value or calculate from geometric data
        return 0.0;
    }
}
