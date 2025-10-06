package fr.airsen.api.dto;

import java.math.BigDecimal;

public class CommuneDTO {
    private Long id;
    private String inseeCode;
    private String name;
    private String departmentCode;
    private String regionCode;
    private Long population;
    private BigDecimal latitude;
    private BigDecimal longitude;

    public CommuneDTO() {}

    public CommuneDTO(Long id, String inseeCode, String name, String departmentCode, String regionCode, Long population) {
        this.id = id;
        this.inseeCode = inseeCode;
        this.name = name;
        this.departmentCode = departmentCode;
        this.regionCode = regionCode;
        this.population = population;
    }

    public CommuneDTO(Long id, String inseeCode, String name, String departmentCode, String regionCode, Long population,
                     BigDecimal latitude, BigDecimal longitude) {
        this.id = id;
        this.inseeCode = inseeCode;
        this.name = name;
        this.departmentCode = departmentCode;
        this.regionCode = regionCode;
        this.population = population;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInseeCode() { return inseeCode; }
    public void setInseeCode(String inseeCode) { this.inseeCode = inseeCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }

    public Long getPopulation() { return population; }
    public void setPopulation(Long population) { this.population = population; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
}