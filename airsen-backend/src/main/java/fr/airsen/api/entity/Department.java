package fr.airsen.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Entité représentant un département français.
 * 
 * Niveau administratif intermédiaire de la hiérarchie géographique française.
 * Appartient à une région et contient des communes.
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

    @Column(name = "department_code", nullable = false, unique = true)
    @NotNull(message = "Department code must not be null")
    @Positive(message = "Department code must be positive")
    private int departmentCode;

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

    public Department(String name, int departmentCode, String regionCode, Region region) {
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

    public int getDepartmentCode() { 
        return departmentCode; 
    }

    public void setDepartmentCode(int departmentCode) { 
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
