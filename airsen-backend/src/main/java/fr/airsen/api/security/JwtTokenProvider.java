package fr.airsen.api.security;

import fr.airsen.api.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token Provider for generating, validating, and parsing JWT tokens.
 * 
 * This component handles all JWT operations for the Airsen authentication system.
 * It provides secure token generation with user claims, expiration management,
 * and comprehensive validation with proper error handling.
 * 
 * Security Features:
 * - HMAC-SHA256 signing algorithm
 * - Configurable token expiration
 * - Comprehensive token validation
 * - Secure secret key management from environment variables
 * - Protection against common JWT vulnerabilities
 * 
 * Token Claims Structure:
 * - sub: User ID (subject)
 * - email: User email address  
 * - role: User role (VISITOR, USER, ADMIN)
 * - iat: Issued at timestamp
 * - exp: Expiration timestamp
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    /**
     * JWT secret key from environment variables.
     * Should be at least 32 characters for HS256 algorithm security.
     */
    @Value("${jwt.secret:default-secret-key-change-in-production-environment}")
    private String jwtSecret;

    /**
     * JWT token expiration time in milliseconds.
     * Default: 24 hours (86400000 ms)
     */
    @Value("${jwt.expiration:86400000}")
    private long jwtExpirationMs;

    /**
     * Algorithm identifier for JWT signing.
     */
    private static final String JWT_ALGORITHM = "HS256";

    /**
     * Generates a JWT token for a user entity.
     * 
     * Creates a secure JWT token containing user identity and role information.
     * The token includes standard JWT claims and custom user claims for authorization.
     * 
     * @param user User entity for token generation
     * @return Signed JWT token string
     * @throws IllegalArgumentException if user is null or invalid
     */
    public String generateToken(User user) {
        if (user == null || user.getId() == null || user.getEmail() == null) {
            throw new IllegalArgumentException("User must have valid ID and email for token generation");
        }

        logger.debug("Generating JWT token for user: {} (ID: {})", user.getEmail(), user.getId());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(user.getId().toString())  // User ID as subject
                .claim("email", user.getEmail())      // User email
                .claim("role", user.getRole().name()) // User role
                .claim("firstName", user.getFirstName()) // User first name
                .claim("lastName", user.getLastName())   // User last name
                .setIssuedAt(now)                     // Token issued timestamp
                .setExpiration(expiryDate)            // Token expiration
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates a JWT token from Spring Security Authentication.
     * 
     * This method is used when working with Spring Security's authentication
     * mechanism after successful login verification.
     * 
     * @param authentication Spring Security authentication object
     * @return Signed JWT token string
     * @throws IllegalArgumentException if authentication is invalid
     */
    public String generateToken(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authentication must be valid for token generation");
        }

        // Extract user information from authentication principal
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) principal;
            return generateTokenFromUserPrincipal(userPrincipal);
        } else if (principal instanceof User) {
            return generateToken((User) principal);
        } else {
            throw new IllegalArgumentException("Unsupported authentication principal type: " + principal.getClass());
        }
    }

    /**
     * Generates JWT token from UserPrincipal (Spring Security UserDetails implementation).
     * 
     * @param userPrincipal UserPrincipal containing user information
     * @return Signed JWT token string
     */
    private String generateTokenFromUserPrincipal(UserPrincipal userPrincipal) {
        logger.debug("Generating JWT token for UserPrincipal: {} (ID: {})", 
                    userPrincipal.getEmail(), userPrincipal.getId());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(userPrincipal.getId().toString())
                .claim("email", userPrincipal.getEmail())
                .claim("role", userPrincipal.getRole().name())
                .claim("firstName", userPrincipal.getFirstName())
                .claim("lastName", userPrincipal.getLastName())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts username (email) from JWT token.
     * 
     * @param token JWT token string
     * @return User email address
     * @throws JwtException if token is invalid or malformed
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("email", String.class);
    }

    /**
     * Extracts user ID from JWT token.
     * 
     * @param token JWT token string
     * @return User ID as Long
     * @throws JwtException if token is invalid or malformed
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        String subjectId = claims.getSubject();
        try {
            return Long.parseLong(subjectId);
        } catch (NumberFormatException e) {
            throw new JwtException("Invalid user ID in token subject: " + subjectId);
        }
    }

    /**
     * Extracts user role from JWT token.
     * 
     * @param token JWT token string
     * @return User role as string
     * @throws JwtException if token is invalid or malformed
     */
    public String getUserRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    /**
     * Extracts token expiration date.
     * 
     * @param token JWT token string
     * @return Token expiration date
     * @throws JwtException if token is invalid or malformed
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * Validates JWT token for authenticity and expiration.
     * 
     * Performs comprehensive token validation including:
     * - Signature verification
     * - Expiration checking
     * - Malformation detection
     * - Claims validation
     * 
     * @param token JWT token string to validate
     * @return true if token is valid and not expired
     */
    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            logger.debug("Token validation failed: token is null or empty");
            return false;
        }

        try {
            Claims claims = getClaimsFromToken(token);
            
            // Additional validation checks
            if (claims.getSubject() == null || claims.getSubject().trim().isEmpty()) {
                logger.warn("Token validation failed: missing or empty subject");
                return false;
            }
            
            String email = claims.get("email", String.class);
            if (email == null || email.trim().isEmpty()) {
                logger.warn("Token validation failed: missing or empty email claim");
                return false;
            }
            
            String role = claims.get("role", String.class);
            if (role == null || role.trim().isEmpty()) {
                logger.warn("Token validation failed: missing or empty role claim");
                return false;
            }

            logger.debug("Token validation successful for user: {}", email);
            return true;

        } catch (ExpiredJwtException e) {
            logger.warn("Token validation failed: token expired at {}", e.getClaims().getExpiration());
            return false;
        } catch (UnsupportedJwtException e) {
            logger.warn("Token validation failed: unsupported JWT token format");
            return false;
        } catch (MalformedJwtException e) {
            logger.warn("Token validation failed: malformed JWT token");
            return false;
        } catch (SecurityException e) {
            logger.warn("Token validation failed: invalid JWT signature");
            return false;
        } catch (IllegalArgumentException e) {
            logger.warn("Token validation failed: illegal argument - {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Token validation failed: unexpected error", e);
            return false;
        }
    }

    /**
     * Checks if JWT token is expired.
     * 
     * @param token JWT token string
     * @return true if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (JwtException e) {
            logger.debug("Unable to check token expiration: {}", e.getMessage());
            return true; // Consider invalid tokens as expired
        }
    }

    /**
     * Gets the JWT expiration time in milliseconds.
     * 
     * @return JWT expiration time in milliseconds
     */
    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    /**
     * Gets the access token expiration time in milliseconds.
     * This is an alias for getJwtExpirationMs() for compatibility with AuthService.
     * 
     * @return access token expiration time in milliseconds
     */
    public long getAccessTokenExpiration() {
        return jwtExpirationMs;
    }

    /**
     * Generates an access token for authentication with email and role.
     * 
     * @param email user email address
     * @param role user role
     * @return signed JWT access token
     */
    public String generateAccessToken(String email, fr.airsen.api.entity.enums.UserRole role) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email must not be null or empty for token generation");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role must not be null for token generation");
        }

        logger.debug("Generating access token for user: {} with role: {}", email, role);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(email)                        // Use email as subject for access tokens
                .claim("email", email)                    // User email
                .claim("role", role.name())               // User role
                .claim("type", "access")                  // Token type
                .setIssuedAt(now)                         // Token issued timestamp
                .setExpiration(expiryDate)                // Token expiration
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates a refresh token for token renewal.
     * 
     * @param email user email address
     * @return signed JWT refresh token with longer expiration
     */
    public String generateRefreshToken(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email must not be null or empty for refresh token generation");
        }

        logger.debug("Generating refresh token for user: {}", email);

        Date now = new Date();
        // Refresh tokens have longer expiration (7 days)
        Date expiryDate = new Date(now.getTime() + (7 * 24 * 60 * 60 * 1000L));

        return Jwts.builder()
                .setSubject(email)                        // Use email as subject for refresh tokens
                .claim("email", email)                    // User email
                .claim("type", "refresh")                 // Token type
                .setIssuedAt(now)                         // Token issued timestamp
                .setExpiration(expiryDate)                // Token expiration (7 days)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts all claims from JWT token.
     * 
     * This method handles the parsing and signature verification of JWT tokens.
     * It throws appropriate exceptions for different validation failures.
     * 
     * @param token JWT token string
     * @return JWT Claims object containing all token claims
     * @throws JwtException for various token validation failures
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Gets the signing key for JWT token operations.
     * 
     * Creates a secure HMAC-SHA256 key from the configured secret.
     * The secret should be at least 32 characters for optimal security.
     * 
     * @return SecretKey for JWT signing and verification
     */
    private SecretKey getSigningKey() {
        // Ensure minimum key length for HS256
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            logger.warn("JWT secret key is shorter than recommended 32 characters");
        }
        
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Gets remaining time until token expiration in milliseconds.
     * 
     * @param token JWT token string
     * @return remaining time in milliseconds, or 0 if expired/invalid
     */
    public long getTokenRemainingTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            long remainingTime = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remainingTime);
        } catch (JwtException e) {
            logger.debug("Unable to get token remaining time: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Extracts user's full name from JWT token.
     * 
     * @param token JWT token string
     * @return Combined first and last name, or email if names not available
     */
    public String getUserFullNameFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String firstName = claims.get("firstName", String.class);
            String lastName = claims.get("lastName", String.class);
            String email = claims.get("email", String.class);
            
            if (firstName != null && lastName != null) {
                return firstName + " " + lastName;
            } else if (firstName != null) {
                return firstName;
            } else if (lastName != null) {
                return lastName;
            } else {
                return email; // Fallback to email
            }
        } catch (JwtException e) {
            logger.debug("Unable to get user full name from token: {}", e.getMessage());
            return "Unknown User";
        }
    }
}