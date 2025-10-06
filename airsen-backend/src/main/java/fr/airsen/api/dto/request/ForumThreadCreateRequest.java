package fr.airsen.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new forum thread.
 *
 * This DTO contains only the essential fields that users need to provide
 * when creating a new thread. Other fields (author, timestamps, counts)
 * are set automatically by the backend.
 */
@Schema(description = "Request to create a new forum thread")
public class ForumThreadCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    @Schema(
        description = "Thread title",
        example = "Discussion about air quality in Paris",
        required = true,
        minLength = 5,
        maxLength = 200
    )
    private String title;

    @NotBlank(message = "Content is required")
    @Size(min = 10, max = 5000, message = "Content must be between 10 and 5000 characters")
    @Schema(
        description = "Thread content/description",
        example = "I noticed high PM2.5 levels in my area. Has anyone else observed similar trends?",
        required = true,
        minLength = 10,
        maxLength = 5000
    )
    private String content;

    @NotNull(message = "Category ID is required")
    @Schema(
        description = "ID of the category this thread belongs to",
        example = "1",
        required = true
    )
    private Long categoryId;

    public ForumThreadCreateRequest() {
    }

    public ForumThreadCreateRequest(String title, String content, Long categoryId) {
        this.title = title;
        this.content = content;
        this.categoryId = categoryId;
    }

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
        return "ForumThreadCreateRequest{" +
                "title='" + title + '\'' +
                ", content='" + content.substring(0, Math.min(50, content.length())) + "...'" +
                ", categoryId=" + categoryId +
                '}';
    }
}
