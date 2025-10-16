package fr.airsen.api.dto.request;

import fr.airsen.api.entity.enums.VoteType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for voting on forum content (threads/messages).
 *
 * This record provides a clean, self-documenting API for voting operations
 * using an enum instead of integer values.
 */
@Schema(description = "Request to vote on forum content")
public record VoteRequest(
    @NotNull(message = "Vote type is required")
    @Schema(
        description = "Type of vote (LIKE or DISLIKE)",
        example = "LIKE",
        required = true,
        allowableValues = {"LIKE", "DISLIKE"}
    )
    VoteType voteType
) {
    /**
     * Convert vote type to integer value for legacy compatibility.
     *
     * @return 1 for LIKE, -1 for DISLIKE
     */
    public int getVoteValue() {
        return voteType != null ? voteType.toInt() : 0;
    }
}