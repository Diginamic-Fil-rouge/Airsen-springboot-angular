package fr.airsen.api.dto;

public class CommuneDTO {
    private Long id;
    private String inseeCode;
    private String name;
    private String departmentCode;
    private String regionCode;
    private Long population;

    public CommuneDTO() {}

    public CommuneDTO(Long id, String inseeCode, String name, String departmentCode, String regionCode, Long population) {
        this.id = id;
        this.inseeCode = inseeCode;
        this.name = name;
        this.departmentCode = departmentCode;
        this.regionCode = regionCode;
        this.population = population;
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
}