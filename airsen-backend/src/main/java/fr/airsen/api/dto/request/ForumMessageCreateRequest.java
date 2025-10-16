package fr.airsen.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new forum message.
 *
 * This DTO contains only the essential field (content) that users need to provide
 * when creating a new message in a thread. Other fields (author, thread, timestamp)
 * are set automatically by the backend.
 */
@Schema(description = "Request to create a new forum message")
public class ForumMessageCreateRequest {

    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 2000, message = "Content must be between 1 and 2000 characters")
    @Schema(
        description = "Message content",
        example = "I agree with your observations. In my neighborhood, we also noticed...",
        required = true,
        minLength = 1,
        maxLength = 2000
    )
    private String content;

    public ForumMessageCreateRequest() {
    }

    public ForumMessageCreateRequest(String content) {
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
        return "ForumMessageCreateRequest{" +
                "content='" + content.substring(0, Math.min(50, content.length())) + "...'" +
                '}';
    }
}
