package fr.airsen.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Représente une commune française.
 */
@Entity
@Table(name = "commune", uniqueConstraints = {
        @UniqueConstraint(columnNames = "insee_code")
})
public class Commune {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank(message = "Le code INSEE est obligatoire")
    @Pattern(regexp = "\\d{5}", message = "Le code INSEE doit être composé de 5 chiffres")
    @Column(name = "insee_code", nullable = false, unique = true, length = 10)
    private String inseeCode;

    @NotBlank(message = "Le nom de la commune ne doit pas être vide")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @PositiveOrZero(message = "La population doit être positive")
    @Column(name = "population")
    private long population;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getInseeCode() { return inseeCode; }
    public void setInseeCode(String inseeCode) { this.inseeCode = inseeCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getPopulation() { return population; }
    public void setPopulation(long population) { this.population = population; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
}
