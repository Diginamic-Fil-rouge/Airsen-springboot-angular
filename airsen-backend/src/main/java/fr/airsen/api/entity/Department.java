package fr.airsen.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Entity representing a French department.
 * 
 * Intermediate administrative level of the French geographic hierarchy.
 * Belongs to a region and contains communes.
 */
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "Department name must not be empty")
    @Size(max = 100, message = "Department name must not exceed 100 characters")
    private String name;

    @Column(name = "department_code", nullable = false, unique = true, length = 3)
    @NotBlank(message = "Department code must not be empty")
    @Size(min = 1, max = 3, message = "Department code must be 1-3 characters")
    private String departmentCode;

    @Column(name = "region_code", length = 10)
    @Size(max = 10, message = "Region code must not exceed 10 characters")
    private String regionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    @NotNull(message = "Department must belong to a valid region")
    private Region region;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Commune> communes;

    public Department() {}

    public Department(String name, String departmentCode, String regionCode, Region region) {
        this.name = name;
        this.departmentCode = departmentCode;
        this.regionCode = regionCode;
        this.region = region;
    }

    // Getters & Setters
    public long getId() {
        return id; 
    }

    public void setId(long id) {
        this.id = id;
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

    public Region getRegion() { 
        return region; 
    }

    public void setRegion(Region region) { 
        this.region = region; 
    }

    public List<Commune> getCommunes() { 
        return communes; 
    }

    public void setCommunes(List<Commune> communes) { 
        this.communes = communes; 
    }
}
