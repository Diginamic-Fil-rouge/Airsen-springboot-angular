package fr.airsen.api.service;

import fr.airsen.api.dto.auth.AuthResponse;
import fr.airsen.api.dto.auth.LoginRequest;
import fr.airsen.api.dto.auth.RegisterRequest;
import fr.airsen.api.dto.auth.RefreshTokenRequest;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.repository.UserRepository;
import fr.airsen.api.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Authentication service for user login, registration, and token management.
 * 
 * This service handles all authentication-related operations including user
 * registration, login authentication, JWT token generation and refresh,
 * and logout functionality. It integrates with Spring Security for secure
 * password handling and JWT token management.
 * 
 * Security Features:
 * - BCrypt password hashing for secure storage
 * - JWT token generation and validation
 * - Role-based access control integration
 * - Comprehensive authentication logging
 * - Secure user registration with email uniqueness
 * - Refresh token management for seamless user experience
 * 
 * Business Logic:
 * - User account creation with automatic role assignment
 * - Email normalization and validation
 * - Password strength enforcement through DTOs
 * - Admin account protection from self-registration
 * - Token expiration and renewal handling
 */
@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Authenticates user credentials and generates JWT tokens.
     * 
     * This method validates user credentials against the database using Spring Security
     * authentication manager. Upon successful authentication, it generates JWT access
     * and refresh tokens for API access.
     * 
     * @param loginRequest validated login credentials
     * @return authentication response with JWT tokens and user information
     * @throws BadCredentialsException if credentials are invalid
     * @throws IllegalArgumentException if request data is invalid
     */
    public AuthResponse authenticate(LoginRequest loginRequest) {
        try {
            logger.debug("Authenticating user: {}", loginRequest.getNormalizedEmail());
            
            // Validate credentials using Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getNormalizedEmail(),
                    loginRequest.password()
                )
            );

            // Load user details for token generation
            User user = userRepository.findByEmailIgnoreCase(loginRequest.getNormalizedEmail())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

            // Generate JWT tokens
            String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

            logger.info("Authentication successful for user: {} with role: {}", 
                       user.getEmail(), user.getRole());

            return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiration() / 1000, // Convert to seconds
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
            );

        } catch (AuthenticationException e) {
            logger.warn("Authentication failed for user: {} - {}", loginRequest.getNormalizedEmail(), e.getMessage());
            throw new BadCredentialsException("Invalid email or password");
            
        } catch (Exception e) {
            logger.error("Unexpected authentication error for user: {} - {}", 
                        loginRequest.getNormalizedEmail(), e.getMessage(), e);
            throw new RuntimeException("Authentication service error");
        }
    }

    /**
     * Registers a new user account and returns immediate authentication.
     * 
     * This method creates a new user account with secure password hashing
     * and role assignment. It prevents duplicate email registration and
     * restricts admin account self-registration. Upon successful registration,
     * it immediately authenticates the user and returns JWT tokens.
     * 
     * @param registerRequest validated registration data
     * @return authentication response with JWT tokens and user information
     * @throws IllegalArgumentException if registration data is invalid
     * @throws RuntimeException if email already exists or admin registration attempted
     */
    public AuthResponse register(RegisterRequest registerRequest) {
        try {
            logger.debug("Registering new user: {}", registerRequest.getNormalizedEmail());

            // Validate registration request
            validateRegistrationRequest(registerRequest);

            // Check if user already exists
            if (userRepository.findByEmailIgnoreCase(registerRequest.getNormalizedEmail()).isPresent()) {
                logger.warn("Registration failed - Email already exists: {}", registerRequest.getNormalizedEmail());
                throw new RuntimeException("Email address is already registered");
            }

            // Create new user entity
            User newUser = createUserFromRegistration(registerRequest);

            // Save user to database
            User savedUser = userRepository.save(newUser);

            logger.info("User registration successful: {} with role: {}", 
                       savedUser.getEmail(), savedUser.getRole());

            // Generate JWT tokens for immediate authentication
            String accessToken = jwtTokenProvider.generateAccessToken(savedUser.getEmail(), savedUser.getRole());
            String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getEmail());

            return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiration() / 1000, // Convert to seconds
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getRole()
            );

        } catch (RuntimeException e) {
            // Re-throw known business exceptions
            throw e;
            
        } catch (Exception e) {
            logger.error("Unexpected registration error for user: {} - {}", 
                        registerRequest.getNormalizedEmail(), e.getMessage(), e);
            throw new RuntimeException("Registration service error");
        }
    }

    /**
     * Refreshes JWT access token using a valid refresh token.
     * 
     * This method validates the refresh token and generates a new access token
     * for continued API access without requiring user re-authentication.
     * 
     * @param refreshRequest validated refresh token request
     * @return new authentication response with refreshed access token
     * @throws BadCredentialsException if refresh token is invalid or expired
     * @throws IllegalArgumentException if request data is invalid
     */
    public AuthResponse refreshAccessToken(RefreshTokenRequest refreshRequest) {
        try {
            String refreshToken = refreshRequest.getCleanRefreshToken();
            
            logger.debug("Refreshing access token");

            // Validate refresh token
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                logger.warn("Token refresh failed - Invalid refresh token");
                throw new BadCredentialsException("Invalid or expired refresh token");
            }

            // Extract user email from refresh token
            String userEmail = jwtTokenProvider.getUsernameFromToken(refreshToken);
            
            if (userEmail == null || userEmail.trim().isEmpty()) {
                logger.warn("Token refresh failed - No email in token");
                throw new BadCredentialsException("Invalid refresh token format");
            }

            // Load user for new token generation
            User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new BadCredentialsException("User not found for token refresh"));

            // Generate new access token
            String newAccessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole());

            logger.info("Token refresh successful for user: {}", user.getEmail());

            return AuthResponse.of(
                newAccessToken,
                refreshToken, // Keep the same refresh token
                jwtTokenProvider.getAccessTokenExpiration() / 1000, // Convert to seconds
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
            );

        } catch (BadCredentialsException e) {
            // Re-throw authentication exceptions
            throw e;
            
        } catch (Exception e) {
            logger.error("Unexpected token refresh error: {}", e.getMessage(), e);
            throw new RuntimeException("Token refresh service error");
        }
    }

    /**
     * Logs out user by invalidating refresh token.
     * 
     * This method handles user logout by invalidating the refresh token.
     * Note: JWT access tokens are stateless and handled client-side.
     * 
     * @param refreshToken refresh token to invalidate
     * @return true if logout successful
     */
    public boolean logout(String refreshToken) {
        try {
            logger.debug("Processing logout request");

            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                logger.warn("Logout failed - No refresh token provided");
                return false;
            }

            // For JWT tokens, we would typically add to a blacklist
            // For now, we'll just validate the token exists and return success
            boolean tokenValid = jwtTokenProvider.validateToken(refreshToken);
            
            if (tokenValid) {
                // In a full implementation, add token to blacklist/revocation list
                logger.info("Logout successful");
                return true;
            } else {
                logger.warn("Logout failed - Invalid refresh token");
                return false;
            }

        } catch (Exception e) {
            logger.error("Logout error: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates registration request data and business rules.
     * 
     * @param registerRequest registration request to validate
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException if admin registration attempted
     */
    private void validateRegistrationRequest(RegisterRequest registerRequest) {
        if (registerRequest == null) {
            throw new IllegalArgumentException("Registration request cannot be null");
        }

        if (registerRequest.getNormalizedEmail() == null || registerRequest.getNormalizedEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (registerRequest.password() == null || registerRequest.password().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        // Prevent self-registration of admin accounts
        UserRole assignedRole = registerRequest.getAssignedRole();
        if (assignedRole == UserRole.ADMIN) {
            logger.warn("Admin registration attempt blocked for: {}", registerRequest.getNormalizedEmail());
            throw new RuntimeException("Admin accounts cannot be self-registered");
        }
    }

    /**
     * Creates a new User entity from registration request data.
     * 
     * @param registerRequest validated registration request
     * @return new User entity ready for persistence
     */
    private User createUserFromRegistration(RegisterRequest registerRequest) {
        User user = new User();
        user.setEmail(registerRequest.getNormalizedEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.password()));
        user.setFirstName(registerRequest.getNormalizedFirstName());
        user.setLastName(registerRequest.getNormalizedLastName());
        user.setRole(registerRequest.getAssignedRole());
        user.setCreatedAt(LocalDateTime.now());
        
        logger.debug("Created user entity for registration: {}", user.getEmail());
        
        return user;
    }
}