package fr.airsen.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * Représente une région française.
 */
@Entity
@Table(name = "region", uniqueConstraints = {
        @UniqueConstraint(columnNames = "code_region")
})
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank(message = "Le nom de la région ne doit pas être vide")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Le code région est obligatoire")
    @Pattern(regexp = "\\d{2}", message = "Le code région doit être un code INSEE à 2 chiffres")
    @Column(name = "code_region", nullable = false, length = 10, unique = true)
    private String codeRegion;

    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Department> departments;

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCodeRegion() { return codeRegion; }
    public void setCodeRegion(String codeRegion) { this.codeRegion = codeRegion; }
    public List<Department> getDepartments() { return departments; }
    public void setDepartments(List<Department> departments) { this.departments = departments; }
}
