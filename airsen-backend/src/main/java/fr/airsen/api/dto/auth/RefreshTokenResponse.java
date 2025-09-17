package fr.airsen.api.dto.auth;

/**
 * Data Transfer Object for token refresh response.
 * 
 * This DTO encapsulates the response data returned after successfully
 * refreshing a JWT token. It provides the new token and its expiration
 * information to the client.
 * 
 * Response Structure (from API specification):
 * - token: New JWT token string
 * - expiresIn: Token expiration time in seconds
 * 
 * Security Considerations:
 * - New token should replace the old one immediately
 * - Old token becomes invalid after refresh
 * - ExpiresIn helps client manage token lifecycle
 */
public class RefreshTokenResponse {

    /**
     * New JWT token string for authentication.
     * 
     * This token should replace the previous token and be included
     * in the Authorization header of subsequent requests.
     */
    private String token;

    /**
     * Token expiration time in seconds.
     * 
     * Indicates how long the new token will remain valid.
     * Client should refresh again before this expiration.
     */
    private Long expiresIn;

    /**
     * Default constructor for JSON serialization.
     */
    public RefreshTokenResponse() {
    }

    /**
     * Constructor with token and expiration in seconds.
     * 
     * @param token new JWT token string
     * @param expiresIn token expiration in seconds
     */
    public RefreshTokenResponse(String token, Long expiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
    }

    /**
     * Constructor with token and expiration in milliseconds.
     * 
     * @param token new JWT token string
     * @param expirationMs token expiration in milliseconds
     */
    public RefreshTokenResponse(String token, long expirationMs) {
        this.token = token;
        this.expiresIn = expirationMs / 1000; // Convert to seconds
    }

    /**
     * Gets the new JWT token string.
     * 
     * @return JWT token for authentication
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the new JWT token string.
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
     * Checks if the refresh response is valid.
     * 
     * @return true if token and expiration are both present and valid
     */
    public boolean isValid() {
        return token != null && !token.trim().isEmpty() && 
               expiresIn != null && expiresIn > 0;
    }

    /**
     * Gets a summary for logging purposes.
     * 
     * Excludes the actual token value for security.
     * 
     * @return sanitized string representation
     */
    public String getLogSummary() {
        return "RefreshTokenResponse{" +
                "hasToken=" + (token != null && !token.isEmpty()) +
                ", tokenLength=" + (token != null ? token.length() : 0) +
                ", expiresIn=" + expiresIn +
                ", expirationTimestamp=" + getExpirationTimestamp() +
                '}';
    }

    /**
     * Returns a string representation of this refresh response.
     * 
     * Excludes the token value for security purposes.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        return "RefreshTokenResponse{" +
                "token='[PROTECTED]'" +
                ", expiresIn=" + expiresIn +
                '}';
    }

    /**
     * Creates a builder for constructing RefreshTokenResponse instances.
     * 
     * @return new RefreshTokenResponse builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing RefreshTokenResponse instances.
     */
    public static class Builder {
        private String token;
        private Long expiresIn;

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
         * Builds the RefreshTokenResponse instance.
         * 
         * @return configured RefreshTokenResponse
         */
        public RefreshTokenResponse build() {
            return new RefreshTokenResponse(token, expiresIn);
        }
    }
}