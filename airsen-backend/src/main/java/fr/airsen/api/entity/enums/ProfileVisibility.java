package fr.airsen.api.entity.enums;

/**
 * Profile visibility settings for user accounts.
 *
 * Controls how much information other users can see on a user's profile page:
 * - HIDDEN: Profile page completely hidden (404 response)
 * - USERNAME_ONLY: Only username/full name visible (default for privacy)
 * - PUBLIC: Full profile information visible (bio, address, etc.)
 */
public enum ProfileVisibility {

    /**
     * Profile page is completely hidden from other users.
     * Access to profile URL returns 404 Not Found.
     * Forum posts still show author name for content context.
     */
    HIDDEN("Profile Hidden"),

    /**
     * Only username/full name is visible on profile page.
     * Other profile fields (bio, address, email, etc.) are not displayed.
     * This is the default setting for new user accounts (privacy by default).
     */
    USERNAME_ONLY("Username Only"),

    /**
     * Full profile information is visible to other users.
     * Includes bio, address, favorite communes, and all public fields.
     * User explicitly opts in to this visibility level.
     */
    PUBLIC("Public Profile");

    private final String displayName;

    ProfileVisibility(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the user-friendly display name for this visibility setting.
     *
     * @return display name for UI presentation
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the default visibility setting for new user accounts.
     * Returns USERNAME_ONLY as per GDPR privacy by default principle.
     *
     * @return default profile visibility (USERNAME_ONLY)
     */
    public static ProfileVisibility getDefaultVisibility() {
        return USERNAME_ONLY;
    }

    /**
     * Checks if profile page should be accessible to other users.
     *
     * @return true if profile can be viewed, false if hidden
     */
    public boolean isProfileAccessible() {
        return this != HIDDEN;
    }

    /**
     * Checks if full profile details should be displayed.
     *
     * @return true if all fields visible, false if limited to username only
     */
    public boolean showFullProfile() {
        return this == PUBLIC;
    }
}
