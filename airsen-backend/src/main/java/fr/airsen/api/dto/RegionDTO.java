package fr.airsen.api.dto;

public class RegionDTO {
    private int id;
    private String name;
    private String regionCode;

    public RegionDTO() {}

    public RegionDTO(int id, String name, String regionCode) {
        this.id = id;
        this.name = name;
        this.regionCode = regionCode;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }
}
