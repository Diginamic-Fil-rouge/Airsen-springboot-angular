package fr.airsen.api.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private int departmentCode;

    // Pour garder la redondance du modèle ER
    private String regionCode;

    @ManyToOne
    @JoinColumn(name = "region_id")
    private Region region;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private List<Commune> communes;

    public Department() {}

    public Department(String name, int departmentCode, String regionCode, Region region) {
        this.name = name;
        this.departmentCode = departmentCode;
        this.regionCode = regionCode;
        this.region = region;
    }

    // Getters & Setters
    public int getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(int departmentCode) { this.departmentCode = departmentCode; }

    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }

    public List<Commune> getCommunes() { return communes; }
    public void setCommunes(List<Commune> communes) { this.communes = communes; }
}
