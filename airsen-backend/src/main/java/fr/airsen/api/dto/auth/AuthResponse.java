package fr.airsen.api.dto.auth;

/**
 * Data Transfer Object for authentication response.
 * 
 * This DTO encapsulates the response data returned after successful
 * authentication (login or registration). It contains user information
 * and the JWT token required for subsequent authenticated requests.
 * 
 * Response Structure (from API specification):
 * - user: User information (id, email, firstName, lastName, role)
 * - token: JWT token string for authentication
 * - expiresIn: Token expiration time in seconds
 * 
 * Security Considerations:
 * - Token should be stored securely by the client
 * - ExpiresIn helps client manage token lifecycle
 * - User info excludes sensitive data like passwords
 */
public class AuthResponse {

    /**
     * User information for the authenticated user.
     * 
     * Contains safe user data that can be exposed to the client
     * for display and authorization purposes.
     */
    private UserDTO user;

    /**
     * JWT token string for authentication.
     * 
     * This token should be included in the Authorization header
     * of subsequent requests as "Bearer {token}".
     */
    private String token;

    /**
     * Token expiration time in seconds.
     * 
     * Indicates how long the token will remain valid.
     * Client should refresh the token before expiration.
     */
    private Long expiresIn;

    /**
     * Default constructor for JSON serialization.
     */
    public AuthResponse() {
    }

    /**
     * Constructor with user, token, and expiration.
     * 
     * @param user authenticated user information
     * @param token JWT token string
     * @param expiresIn token expiration in seconds
     */
    public AuthResponse(UserDTO user, String token, Long expiresIn) {
        this.user = user;
        this.token = token;
        this.expiresIn = expiresIn;
    }

    /**
     * Constructor with user and token (calculates expiration).
     * 
     * @param user authenticated user information
     * @param token JWT token string
     * @param expirationMs token expiration in milliseconds
     */
    public AuthResponse(UserDTO user, String token, long expirationMs) {
        this.user = user;
        this.token = token;
        this.expiresIn = expirationMs / 1000; // Convert to seconds
    }

    /**
     * Gets the authenticated user information.
     * 
     * @return user data transfer object
     */
    public UserDTO getUser() {
        return user;
    }

    /**
     * Sets the authenticated user information.
     * 
     * @param user user data transfer object to set
     */
    public void setUser(UserDTO user) {
        this.user = user;
    }

    /**
     * Gets the JWT token string.
     * 
     * @return JWT token for authentication
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the JWT token string.
     * 
     * @param token JWT token to set
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Gets the token expiration time in seconds.
     * 
     * @return seconds until token expires
     */
    public Long getExpiresIn() {
        return expiresIn;
    }

    /**
     * Sets the token expiration time in seconds.
     * 
     * @param expiresIn seconds until token expires
     */
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Calculates the expiration timestamp.
     * 
     * Returns the Unix timestamp when the token will expire.
     * 
     * @return expiration timestamp in seconds since epoch
     */
    public Long getExpirationTimestamp() {
        if (expiresIn == null) {
            return null;
        }
        return System.currentTimeMillis() / 1000 + expiresIn;
    }

    /**
     * Checks if the token information is complete.
     * 
     * @return true if user, token, and expiration are all present
     */
    public boolean isComplete() {
        return user != null && 
               token != null && !token.trim().isEmpty() && 
               expiresIn != null && expiresIn > 0;
    }

    /**
     * Gets a summary of the authentication response for logging.
     * 
     * Excludes the actual token value for security.
     * 
     * @return sanitized string representation
     */
    public String getLogSummary() {
        return "AuthResponse{" +
                "userId=" + (user != null ? user.getId() : null) +
                ", userEmail=" + (user != null ? user.getEmail() : null) +
                ", userRole=" + (user != null ? user.getRole() : null) +
                ", hasToken=" + (token != null && !token.isEmpty()) +
                ", expiresIn=" + expiresIn +
                '}';
    }

    /**
     * Returns a string representation of this authentication response.
     * 
     * Excludes the token value for security purposes.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        return "AuthResponse{" +
                "user=" + user +
                ", token='[PROTECTED]'" +
                ", expiresIn=" + expiresIn +
                '}';
    }

    /**
     * Creates a builder for constructing AuthResponse instances.
     * 
     * @return new AuthResponse builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing AuthResponse instances.
     */
    public static class Builder {
        private UserDTO user;
        private String token;
        private Long expiresIn;

        /**
         * Sets the user information.
         * 
         * @param user user data transfer object
         * @return this builder
         */
        public Builder user(UserDTO user) {
            this.user = user;
            return this;
        }

        /**
         * Sets the JWT token.
         * 
         * @param token JWT token string
         * @return this builder
         */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * Sets the token expiration in seconds.
         * 
         * @param expiresIn seconds until expiration
         * @return this builder
         */
        public Builder expiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        /**
         * Sets the token expiration from milliseconds.
         * 
         * @param expirationMs milliseconds until expiration
         * @return this builder
         */
        public Builder expiresInMs(long expirationMs) {
            this.expiresIn = expirationMs / 1000;
            return this;
        }

        /**
         * Builds the AuthResponse instance.
         * 
         * @return configured AuthResponse
         */
        public AuthResponse build() {
            return new AuthResponse(user, token, expiresIn);
        }
    }
}