package fr.airsen.api.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Commune {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String inseeCode;
    private String name;
    private String departmentCode;
    private String regionCode;

    private long population;

    @ManyToMany
    @JoinTable(name = "user_favorite", joinColumns = @JoinColumn(name = "commune_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> favoris = new HashSet<>();


    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    public Commune() {}

    public Commune(String inseeCode, String name, String departmentCode, String regionCode, long population, Department department) {
        this.inseeCode = inseeCode;
        this.name = name;
        this.departmentCode = departmentCode;
        this.regionCode = regionCode;
        this.population = population;
        this.department = department;
    }

    // Getters & Setters
    public int getId() { return id; }

    public String getInseeCode() { return inseeCode; }
    public void setInseeCode(String inseeCode) { this.inseeCode = inseeCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }

    public long getPopulation() { return population; }
    public void setPopulation(long population) { this.population = population; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
}
