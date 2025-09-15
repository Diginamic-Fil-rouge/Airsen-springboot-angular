package fr.airsen.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * Représente un département français.
 */
@Entity
@Table(name = "department", uniqueConstraints = {
        @UniqueConstraint(columnNames = "code_department")
})
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank(message = "Le nom du département ne doit pas être vide")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Le code département est obligatoire")
    @Pattern(regexp = "\\d{2,3}[A-Z]?", message = "Le code département doit suivre le format INSEE")
    @Column(name = "code_department", nullable = false, unique = true, length = 10)
    private String codeDepartment;

    @ManyToOne
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Commune> communes;

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCodeDepartment() { return codeDepartment; }
    public void setCodeDepartment(String codeDepartment) { this.codeDepartment = codeDepartment; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    public List<Commune> getCommunes() { return communes; }
    public void setCommunes(List<Commune> communes) { this.communes = communes; }
}
