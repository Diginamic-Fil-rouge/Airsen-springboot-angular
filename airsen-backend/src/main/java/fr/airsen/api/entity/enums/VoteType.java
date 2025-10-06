package fr.airsen.api.entity.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enum representing vote types for forum threads and messages.
 *
 * This enum provides a clear, type-safe way to handle voting operations
 * instead of using integer values (-1, 1).
 */
@Schema(description = "Vote type for forum content")
public enum VoteType {
    /**
     * Positive vote (like, upvote)
     */
    @Schema(description = "Positive vote (like/upvote)")
    LIKE,

    /**
     * Negative vote (dislike, downvote)
     */
    @Schema(description = "Negative vote (dislike/downvote)")
    DISLIKE;

    /**
     * Convert VoteType to integer value for database storage.
     *
     * @return 1 for LIKE, -1 for DISLIKE
     */
    public int toInt() {
        return this == LIKE ? 1 : -1;
    }

    /**
     * Convert integer value to VoteType.
     *
     * @param value integer value (1 or -1)
     * @return corresponding VoteType
     * @throws IllegalArgumentException if value is not 1 or -1
     */
    public static VoteType fromInt(int value) {
        return switch (value) {
            case 1 -> LIKE;
            case -1 -> DISLIKE;
            default -> throw new IllegalArgumentException("Invalid vote value: " + value + ". Must be 1 (LIKE) or -1 (DISLIKE)");
        };
    }
}
