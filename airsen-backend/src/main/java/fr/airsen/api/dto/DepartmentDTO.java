package fr.airsen.api.dto;

public class DepartmentDTO {
    private Long id;
    private String name;
    private String departmentCode;
    private String regionCode;

    public DepartmentDTO() {}

    public DepartmentDTO(Long id, String name, String departmentCode, String regionCode) {
        this.id = id;
        this.name = name;
        this.departmentCode = departmentCode;
        this.regionCode = regionCode;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }
}

