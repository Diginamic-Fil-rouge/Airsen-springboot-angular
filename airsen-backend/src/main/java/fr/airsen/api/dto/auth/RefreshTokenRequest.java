package fr.airsen.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh token request DTO for token renewal.
 * 
 * This record represents the data required for refreshing JWT access tokens.
 * It contains the refresh token needed to generate new access tokens without
 * requiring user re-authentication.
 * 
 * Security Features:
 * - Refresh token validation
 * - Input sanitization through Bean Validation
 * - Secure toString implementation without token exposure
 * - Token cleanup utilities
 */
public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {
    
    /**
     * Gets cleaned refresh token without Bearer prefix or whitespace.
     * 
     * This method removes common token prefixes and normalizes the token
     * for consistent processing throughout the application.
     * 
     * @return cleaned refresh token
     */
    public String getCleanRefreshToken() {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return null;
        }
        
        String cleaned = refreshToken.trim();
        
        // Remove "Bearer " prefix if present (case-insensitive)
        if (cleaned.toLowerCase().startsWith("bearer ")) {
            cleaned = cleaned.substring(7).trim();
        }
        
        return cleaned;
    }
    
    /**
     * Validates that the refresh token is present and not empty.
     * 
     * @return true if refresh token is valid
     */
    public boolean hasValidRefreshToken() {
        String cleaned = getCleanRefreshToken();
        return cleaned != null && !cleaned.isEmpty();
    }
    
    /**
     * Secure toString implementation that excludes the actual token.
     * 
     * Shows only the token length for debugging purposes while maintaining security.
     * 
     * @return string representation without sensitive token data
     */
    @Override
    public String toString() {
        if (refreshToken == null) {
            return "RefreshTokenRequest[refreshToken=null]";
        }
        
        String cleaned = getCleanRefreshToken();
        int length = cleaned != null ? cleaned.length() : 0;
        
        return String.format("RefreshTokenRequest[refreshToken=***(%d chars)]", length);
    }
}