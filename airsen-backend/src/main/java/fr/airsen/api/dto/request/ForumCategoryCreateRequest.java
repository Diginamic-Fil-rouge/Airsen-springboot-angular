package fr.airsen.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new forum category.
 *
 * This DTO contains only the essential fields that administrators need to provide
 * when creating a new category. The ID and other metadata are generated automatically.
 */
@Schema(description = "Request to create a new forum category")
public class ForumCategoryCreateRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    @Schema(
        description = "Category name",
        example = "Air Quality Discussion",
        required = true,
        minLength = 3,
        maxLength = 50
    )
    private String name;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    @Schema(
        description = "Category description",
        example = "Discuss air quality issues, measurements, and concerns in your region",
        required = true,
        minLength = 10,
        maxLength = 500
    )
    private String description;

    @NotBlank(message = "Color is required")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex code (e.g., #FF5733)")
    @Schema(
        description = "Category color in hex format",
        example = "#3B82F6",
        required = true,
        pattern = "^#[0-9A-Fa-f]{6}$"
    )
    private String color;

    public ForumCategoryCreateRequest() {
    }

    public ForumCategoryCreateRequest(String name, String description, String color) {
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
        return "ForumCategoryCreateRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", color='" + color + '\'' +
                '}';
    }
}
