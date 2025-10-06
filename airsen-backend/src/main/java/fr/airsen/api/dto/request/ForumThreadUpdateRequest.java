package fr.airsen.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing forum thread.
 *
 * This DTO contains only the fields that can be updated by the thread author.
 * The author, timestamps, and counts cannot be modified.
 */
@Schema(description = "Request to update an existing forum thread")
public class ForumThreadUpdateRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    @Schema(
        description = "Updated thread title",
        example = "Discussion about air quality in Paris - Updated",
        required = true,
        minLength = 5,
        maxLength = 200
    )
    private String title;

    @NotBlank(message = "Content is required")
    @Size(min = 10, max = 5000, message = "Content must be between 10 and 5000 characters")
    @Schema(
        description = "Updated thread content/description",
        example = "I noticed high PM2.5 levels in my area. UPDATE: The situation has improved...",
        required = true,
        minLength = 10,
        maxLength = 5000
    )
    private String content;

    @Schema(
        description = "ID of the category (optional - only change if moving to different category)",
        example = "2"
    )
    private Long categoryId;

    public ForumThreadUpdateRequest() {
    }

    public ForumThreadUpdateRequest(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public ForumThreadUpdateRequest(String title, String content, Long categoryId) {
        this.title = title;
        this.content = content;
        this.categoryId = categoryId;
    }

    // Getters and Setters

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public String toString() {
        return "ForumThreadUpdateRequest{" +
                "title='" + title + '\'' +
                ", content='" + content.substring(0, Math.min(50, content.length())) + "...'" +
                ", categoryId=" + categoryId +
                '}';
    }
}
