package fr.airsen.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Objects;

/**
 * This entity represents a category in the forum.
 */
@Entity
@Table(name = "forum_categories")
public class ForumCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, length = 10)
    private long id;

    @Column(name = "name", nullable = false, length = 50)
    @Size(min = 3, message = "Name must be at least 3 characters long")
    private String name;

    @Column(name = "description", nullable = false, length = 65535)
    private String description;

    @Column(name = "color", nullable = false, length = 7)
    @Size(min = 7, max = 7, message = "Color must be a valid hex code")
    private String color;

    @OneToMany(mappedBy = "category", fetch = FetchType.EAGER)
    private List<ForumThread> threads;

    public ForumCategory() {
    }

    public ForumCategory(String name, String description, String color, List<ForumThread> threads) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.threads = threads;
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public List<ForumThread> getThreads() {
        return threads;
    }

    public void setThreads(List<ForumThread> threads) {
        this.threads = threads;
    }

    @Override
    public String toString() {
        return "ForumCategory{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", color='" + color + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ForumCategory that)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
