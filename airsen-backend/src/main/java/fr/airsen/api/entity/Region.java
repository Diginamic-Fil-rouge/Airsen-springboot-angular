package fr.airsen.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Entité représentant une région française.
 * 
 * Correspond au niveau administratif supérieur de la hiérarchie géographique française.
 * Contient les départements et respecte les codes INSEE officiels.
 */
@Entity
@Table(name = "regions")
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "Region name must not be empty")
    @Size(max = 100, message = "Region name must not exceed 100 characters")
    private String name;

    @Column(name = "region_code", unique = true, nullable = false, length = 10)
    @NotBlank(message = "Region code must not be empty")
    @Size(max = 10, message = "Region code must not exceed 10 characters")
    private String regionCode;

    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Department> departments;

    public Region() {}

    public Region(String name, String regionCode) {
        this.name = name;
        this.regionCode = regionCode;
    }

    // Getters & Setters
    public long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() { 
        return name; 
    }

    public void setName(String name) { 
        this.name = name; 
    }

    public String getRegionCode() { 
        return regionCode; 
    }

    public void setRegionCode(String regionCode) { 
        this.regionCode = regionCode; 
    }

    public List<Department> getDepartments() { 
        return departments; 
    }

    public void setDepartments(List<Department> departments) { 
        this.departments = departments; 
    }
}
