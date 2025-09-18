package fr.airsen.api.DTO;

import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumThread;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;


/**
 * DTO to represent a forum category
 */
public class ForumCategoryDTO {

    private long id;

    private String name;

    private String description;

    private String color;

    private List<ForumThreadDTO> threads;

    public ForumCategoryDTO() {
    }

    public ForumCategoryDTO(ForumCategory forumCategory, boolean withThreads) {
        this.id = forumCategory.getId();
        this.name = forumCategory.getName();
        this.description = forumCategory.getDescription();
        this.color = forumCategory.getColor();
        if (withThreads) {
            this.threads = new ArrayList<>();
            if (forumCategory.getThreads() != null) {
                for (ForumThread forumThread : forumCategory.getThreads()) {
                    this.threads.add(new ForumThreadDTO(forumThread, false));
                }
            }
        }
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

    public List<ForumThreadDTO> getThreads() {
        return threads;
    }

    public void setThreads(List<ForumThreadDTO> threads) {
        this.threads = threads;
    }
}