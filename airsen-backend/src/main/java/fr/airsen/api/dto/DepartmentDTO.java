package fr.airsen.api.dto;

public class DepartmentDTO {
    private int id;
    private String name;
    private int departmentCode;
    private String regionCode;

    public DepartmentDTO() {}

    public DepartmentDTO(int id, String name, int departmentCode, String regionCode) {
        this.id = id;
        this.name = name;
        this.departmentCode = departmentCode;
        this.regionCode = regionCode;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(int departmentCode) { this.departmentCode = departmentCode; }

    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }
}

