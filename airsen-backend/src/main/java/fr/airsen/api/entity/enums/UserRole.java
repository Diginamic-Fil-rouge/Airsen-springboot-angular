package fr.airsen.api.entity.enums;

/**
 * Enumeration of user roles in the Airsen application.
 * 
 * Defines authorization levels for accessing application features
 * according to project specifications.
 */
public enum UserRole {

    /**
     * Visitor - Very limited access to public pages.
     * 
     * Permissions:
     * - Access to homepage only
     * - Read forum discussions (no participation)
     * - No access to air quality data
     * - No access to weather data
     * - No access to geographic data
     */
    VISITOR("visitor", "Visitor", "Limited access to homepage and forum reading"),

    /**
     * Logged User - Full access to data and personalized features.
     * 
     * Permissions (full access):
     * - View air quality data
     * - View weather data
     * - Access geographic data (maps, communes)
     * - Manage user profile
     * - Add/remove favorites (max 10)
     * - Configure personalized alerts (email only)
     * - Full forum participation (post, vote)
     * - Export personalized data (5/day, 10/month, 15/year)
     */
    USER("user", "Logged User", "Full access to data and personalized features"),

    /**
     * Administrator - Full access to application management.
     * 
     * Permissions inherited from USER plus:
     * - User management
     * - Forum moderation
     * - External data source configuration
     * - Access to usage metrics and statistics
     * - Application settings management
     */
    ADMIN("admin", "Administrator", "Full access to application management");

    private final String value;
    private final String displayName;
    private final String description;

    UserRole(String value, String displayName, String description) {
        this.value = value;
        this.displayName = displayName;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if current role has permissions greater than or equal to specified role.
     * 
     * @param role role to compare
     * @return true if current role has greater or equal permissions
     */
    public boolean hasPermissionsOf(UserRole role) {
        return this.ordinal() >= role.ordinal();
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isAuthenticated() {
        return this == USER || this == ADMIN;
    }

    /**
     * Gets default role for new users.
     * 
     * @return default role according to specification (USER for new registrations)
     */
    public static UserRole getDefaultRole() {
        return USER;
    }

    /**
     * Gets role by its database value.
     * 
     * @param value value stored in database
     * @return corresponding role or null if not found
     */
    public static UserRole fromValue(String value) {
        if (value == null) return null;
        
        for (UserRole role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        return null;
    }

    public boolean canAccessAirQualityData() {
        return this == USER || this == ADMIN;
    }

    public boolean canManageUsers() {
        return this == ADMIN;
    }

    public boolean canPostInForum() {
        return this == USER || this == ADMIN;
    }

    @Override
    public String toString() {
        return displayName;
    }
}