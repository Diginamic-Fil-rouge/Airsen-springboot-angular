package fr.airsen.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for voting operations.
 * 
 * This record represents a vote request for forum threads and messages.
 * The like value indicates the type of vote: 1 for like, -1 for dislike.
 */
@Schema(description = "Vote request for forum content")
public record VoteRequest(
    @Schema(description = "Vote value: 1 for like, -1 for dislike", example = "1")
    @NotNull(message = "Like value is required")
    @Min(value = -1, message = "Like value must be -1 or 1")
    @Max(value = 1, message = "Like value must be -1 or 1")
    Integer likeValue
) {
}