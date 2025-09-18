package fr.airsen.api.dto.auth;

import fr.airsen.api.entity.enums.UserRole;

import java.time.LocalDateTime;

/**
 * Authentication response DTO containing JWT tokens and user information.
 * 
 * This record represents the response returned after successful authentication
 * or registration. It includes JWT tokens for API access and essential user
 * information for the frontend application.
 * 
 * Security Features:
 * - JWT access token for API authentication
 * - Refresh token for token renewal
 * - Token expiration information
 * - User role information for authorization
 * - No sensitive user data exposure
 */
public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn,
    String userEmail,
    String userFirstName,
    String userLastName,
    UserRole userRole,
    LocalDateTime issuedAt
) {
    
    /**
     * Creates a new AuthResponse with Bearer token type.
     * 
     * @param accessToken JWT access token
     * @param refreshToken JWT refresh token
     * @param expiresIn expiration time in seconds
     * @param userEmail user email address
     * @param userFirstName user first name
     * @param userLastName user last name
     * @param userRole user role for authorization
     * @return new AuthResponse instance
     */
    public static AuthResponse of(String accessToken, String refreshToken, Long expiresIn,
                                  String userEmail, String userFirstName, String userLastName,
                                  UserRole userRole) {
        return new AuthResponse(
            accessToken,
            refreshToken,
            "Bearer",
            expiresIn,
            userEmail,
            userFirstName,
            userLastName,
            userRole,
            LocalDateTime.now()
        );
    }
    
    /**
     * Creates a new AuthResponse from existing token with updated access token.
     * 
     * @param newAccessToken new JWT access token
     * @param originalResponse original authentication response
     * @param expiresIn new expiration time in seconds
     * @return new AuthResponse with updated access token
     */
    public static AuthResponse refreshToken(String newAccessToken, AuthResponse originalResponse, Long expiresIn) {
        return new AuthResponse(
            newAccessToken,
            originalResponse.refreshToken(),
            originalResponse.tokenType(),
            expiresIn,
            originalResponse.userEmail(),
            originalResponse.userFirstName(),
            originalResponse.userLastName(),
            originalResponse.userRole(),
            LocalDateTime.now()
        );
    }
    
    /**
     * Gets the full user name (first + last).
     * 
     * @return formatted full name
     */
    public String getFullUserName() {
        if (userFirstName == null && userLastName == null) {
            return "Unknown User";
        }
        return String.format("%s %s", 
                            userFirstName != null ? userFirstName : "",
                            userLastName != null ? userLastName : "").trim();
    }
    
    /**
     * Checks if the token has expired based on issued time and expires in.
     * 
     * @return true if token is expired
     */
    public boolean isExpired() {
        if (issuedAt == null || expiresIn == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(issuedAt.plusSeconds(expiresIn));
    }
}