package fr.airsen.api.security;

import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token Provider for generating and validating JWT tokens.
 * 
 * This component handles all JWT operations including token generation,
 * validation, parsing, and claims extraction. It integrates with the
 * existing User entity and role system to provide stateless authentication.
 * 
 * Security Features:
 * - HMAC-SHA256 signing algorithm
 * - Configurable token expiration
 * - Secure secret key management from environment variables
 * - Comprehensive error handling and logging
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String AUTHORITIES_KEY = "roles";
    private static final String USER_ID_KEY = "userId";
    private static final String EMAIL_KEY = "email";

    @Value("${airsen.jwt.secret}")
    private String jwtSecret;

    @Value("${airsen.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    /**
     * Generates a JWT token for the authenticated user.
     * 
     * @param authentication Spring Security authentication object
     * @return JWT token string
     * @throws IllegalArgumentException if authentication is null or invalid
     */
    public String generateToken(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authentication cannot be null");
        }

        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put(EMAIL_KEY, userPrincipal.getUsername());
        claims.put(AUTHORITIES_KEY, userPrincipal.getAuthorities().iterator().next().getAuthority());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates a JWT token for a User entity.
     * 
     * @param user User entity
     * @return JWT token string
     * @throws IllegalArgumentException if user is null or has invalid data
     */
    public String generateToken(User user) {
        if (user == null || user.getEmail() == null || user.getRole() == null) {
            throw new IllegalArgumentException("User and user data cannot be null");
        }

        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID_KEY, user.getId());
        claims.put(EMAIL_KEY, user.getEmail());
        claims.put(AUTHORITIES_KEY, user.getRole().name());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the username (email) from JWT token.
     * 
     * @param token JWT token string
     * @return username (email) from token
     * @throws JwtException if token is invalid or expired
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    /**
     * Extracts the user ID from JWT token.
     * 
     * @param token JWT token string
     * @return user ID from token, or null if not present
     * @throws JwtException if token is invalid or expired
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        Object userIdClaim = claims.get(USER_ID_KEY);
        if (userIdClaim instanceof Number) {
            return ((Number) userIdClaim).longValue();
        }
        return null;
    }

    /**
     * Extracts the user role from JWT token.
     * 
     * @param token JWT token string
     * @return UserRole from token, or null if not present or invalid
     * @throws JwtException if token is invalid or expired
     */
    public UserRole getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        String roleString = (String) claims.get(AUTHORITIES_KEY);
        if (roleString != null) {
            try {
                return UserRole.valueOf(roleString);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid role in token: {}", roleString);
                return null;
            }
        }
        return null;
    }

    /**
     * Validates a JWT token.
     * 
     * @param token JWT token string
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (SecurityException ex) {
            logger.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Checks if a JWT token is expired.
     * 
     * @param token JWT token string
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            logger.error("Error checking token expiration: {}", e.getMessage());
            return true; // Consider invalid tokens as expired
        }
    }

    /**
     * Gets the expiration date from a JWT token.
     * 
     * @param token JWT token string
     * @return expiration date, or null if token is invalid
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getExpiration();
        } catch (Exception e) {
            logger.error("Error extracting expiration date: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gets the signing key for JWT operations.
     * 
     * @return SecretKey for JWT signing and verification
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Gets the configured JWT expiration time in milliseconds.
     * 
     * @return JWT expiration time in milliseconds
     */
    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }
}