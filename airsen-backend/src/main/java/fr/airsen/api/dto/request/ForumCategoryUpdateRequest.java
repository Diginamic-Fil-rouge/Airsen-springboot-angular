package fr.airsen.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing forum category.
 *
 * This DTO allows partial updates - all fields are optional.
 * Only non-null fields will be updated in the database.
 */
@Schema(description = "Request to update an existing forum category")
public class ForumCategoryUpdateRequest {

    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    @Schema(
        description = "Updated category name (optional)",
        example = "Air Quality Discussion",
        minLength = 3,
        maxLength = 50
    )
    private String name;

    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    @Schema(
        description = "Updated category description (optional)",
        example = "Discuss air quality issues, measurements, and concerns in your region",
        minLength = 10,
        maxLength = 500
    )
    private String description;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex code (e.g., #FF5733)")
    @Schema(
        description = "Updated category color in hex format (optional)",
        example = "#3B82F6",
        pattern = "^#[0-9A-Fa-f]{6}$"
    )
    private String color;

    public ForumCategoryUpdateRequest() {
    }

    public ForumCategoryUpdateRequest(String name, String description, String color) {
        this.name = name;
        this.description = description;
        this.color = color;
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

    @Override
    public String toString() {
        return "ForumCategoryUpdateRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", color='" + color + '\'' +
                '}';
    }
}
