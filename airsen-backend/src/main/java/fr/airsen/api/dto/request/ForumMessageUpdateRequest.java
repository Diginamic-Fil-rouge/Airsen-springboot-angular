package fr.airsen.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing forum message.
 *
 * This DTO contains only the content field that can be updated.
 * The author, thread, and timestamp cannot be modified.
 */
@Schema(description = "Request to update an existing forum message")
public class ForumMessageUpdateRequest {

    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 2000, message = "Content must be between 1 and 2000 characters")
    @Schema(
        description = "Updated message content",
        example = "I agree with your observations. UPDATE: The situation has improved...",
        required = true,
        minLength = 1,
        maxLength = 2000
    )
    private String content;

    public ForumMessageUpdateRequest() {
    }

    public ForumMessageUpdateRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "ForumMessageUpdateRequest{" +
                "content='" + content.substring(0, Math.min(50, content.length())) + "...'" +
                '}';
    }
}
