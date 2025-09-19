package fr.airsen.api.dto;

public class RegionDTO {
    private Long id;
    private String name;
    private String regionCode;

    public RegionDTO() {}

    public RegionDTO(Long id, String name, String regionCode) {
        this.id = id;
        this.name = name;
        this.regionCode = regionCode;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }
}
