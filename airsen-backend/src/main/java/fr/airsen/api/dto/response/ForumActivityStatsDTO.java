package fr.airsen.api.dto.response;

/**
 * Data Transfer Object for user forum activity statistics.
 *
 * <p>This immutable record contains aggregated statistics about a user's participation
 * in the AIRSEN forum, including thread and message counts. It is used as part of the
 * public user profile to showcase community engagement.</p>
 *
 * <p><strong>Privacy Note:</strong> These statistics are only exposed for users with
 * PUBLIC profile visibility. HIDDEN and USERNAME_ONLY profiles do not expose activity stats.</p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * ForumActivityStatsDTO stats = new ForumActivityStatsDTO(15L, 87L);
 * System.out.println(stats.threadCount());  // 15
 * System.out.println(stats.messageCount()); // 87
 * }</pre>
 *
 * @param threadCount total number of forum threads created by the user
 * @param messageCount total number of forum messages/replies posted by the user
 *
 * @see PublicUserProfileDTO
 * @since 1.0
 */
public record ForumActivityStatsDTO(
        Long threadCount,
        Long messageCount
) {
    /**
     * Compact constructor with validation.
     *
     * <p>Ensures that counts are non-negative. Negative values are replaced with 0
     * to prevent invalid data from corrupting the DTO.</p>
     *
     * @param threadCount total number of threads (must be >= 0)
     * @param messageCount total number of messages (must be >= 0)
     */
    public ForumActivityStatsDTO {
        // Validation: ensure non-negative counts
        if (threadCount == null || threadCount < 0) {
            threadCount = 0L;
        }
        if (messageCount == null || messageCount < 0) {
            messageCount = 0L;
        }
    }

    /**
     * Calculates the total number of forum contributions (threads + messages).
     *
     * <p>This provides a quick metric for overall forum activity level.</p>
     *
     * @return sum of thread count and message count
     */
    public Long getTotalContributions() {
        return threadCount + messageCount;
    }

    /**
     * Checks if the user has any forum activity.
     *
     * <p>Returns true if the user has created at least one thread or message.</p>
     *
     * @return true if user has forum activity, false otherwise
     */
    public boolean hasActivity() {
        return threadCount > 0 || messageCount > 0;
    }
}
