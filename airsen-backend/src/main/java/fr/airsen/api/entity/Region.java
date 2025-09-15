package fr.airsen.api.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @Column(unique = true, nullable = false)
    private String regionCode;

    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL)
    private List<Department> departments;

    public Region() {}

    public Region(String name, String regionCode) {
        this.name = name;
        this.regionCode = regionCode;
    }

    // Getters & Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }

    public List<Department> getDepartments() { return departments; }
    public void setDepartments(List<Department> departments) { this.departments = departments; }
}
