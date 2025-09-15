package fr.airsen.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Entité représentant une commune française.
 * 
 * Niveau administratif le plus bas de la hiérarchie géographique française.
 * Contient les données démographiques et géographiques pour l'affichage
 * sur carte interactive et la recherche de proximité.
 */
@Entity
@Table(name = "communes")
public class Commune {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

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
    private Set<WeatherData> weatherData = new HashSet<>();

    @OneToMany(mappedBy = "commune", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<AirQuality> airQuality = new HashSet<>();

    @ManyToMany(mappedBy = "favoris", fetch = FetchType.LAZY)
    private Set<User> favoriteUsers = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    @NotNull(message = "Commune must belong to a valid department")
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
    public int getId() { 
        return id; 
    }

    public void setId(int id) {
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

    public Set<User> getFavoriteUsers() {
        return favoriteUsers;
    }

    public void setFavoriteUsers(Set<User> favoriteUsers) {
        this.favoriteUsers = favoriteUsers;
    }
}
