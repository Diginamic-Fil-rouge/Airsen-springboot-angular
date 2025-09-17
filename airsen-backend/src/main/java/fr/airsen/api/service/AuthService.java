package fr.airsen.api.service;

import fr.airsen.api.dto.auth.*;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.repository.UserRepository;
import fr.airsen.api.security.JwtTokenProvider;
import fr.airsen.api.security.UserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service class for handling authentication operations.
 * 
 * This service provides business logic for user registration, login, logout,
 * and token refresh operations. It integrates with Spring Security, JWT tokens,
 * and the user management system to provide secure authentication services.
 * 
 * Key Responsibilities:
 * - User registration with validation and password hashing
 * - User authentication and JWT token generation
 * - Token refresh and validation
 * - User data conversion to DTOs
 * - Integration with Spring Security authentication framework
 * 
 * Security Features:
 * - BCrypt password hashing with secure strength
 * - Email uniqueness validation
 * - Input sanitization and validation
 * - Comprehensive error handling without information leakage
 * - Transactional database operations
 */
@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Registers a new user account.
     * 
     * This method handles the complete user registration process including
     * validation, password hashing, and initial JWT token generation.
     * 
     * Process:
     * 1. Validate input data and email uniqueness
     * 2. Hash the password using BCrypt
     * 3. Create and save the new user entity
     * 4. Generate JWT token for immediate authentication
     * 5. Return authentication response with user data and token
     * 
     * @param registerRequest user registration data
     * @return AuthResponse with user information and JWT token
     * @throws IllegalArgumentException if email already exists or validation fails
     */
    public AuthResponse registerUser(RegisterRequest registerRequest) {
        logger.info("Processing user registration for email: {}", 
                   registerRequest.getNormalizedEmail());

        // Validate email uniqueness
        String normalizedEmail = registerRequest.getNormalizedEmail();
        if (userRepository.existsByEmail(normalizedEmail)) {
            logger.warn("Registration failed: email already exists: {}", normalizedEmail);
            throw new IllegalArgumentException("Email already exists");
        }

        // Additional password validation
        if (!registerRequest.isPasswordSecure()) {
            logger.warn("Registration failed: password does not meet security requirements for: {}", 
                       normalizedEmail);
            throw new IllegalArgumentException(
                "Password must be at least 8 characters and contain uppercase, lowercase, and digit characters"
            );
        }

        // Create new user entity
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName().trim());
        user.setLastName(registerRequest.getLastName().trim());
        user.setRole(UserRole.USER); // Default role for new users
        user.setCreatedAt(LocalDateTime.now());

        // Save user to database
        try {
            user = userRepository.save(user);
            logger.info("Successfully created user account with ID: {} for email: {}", 
                       user.getId(), normalizedEmail);
        } catch (Exception e) {
            logger.error("Failed to save user during registration for email: {}", normalizedEmail, e);
            throw new RuntimeException("Failed to create user account");
        }

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user);
        long expirationMs = jwtTokenProvider.getJwtExpirationMs();

        // Convert to DTO and create response
        UserDTO userDTO = convertToUserDTO(user);
        AuthResponse response = new AuthResponse(userDTO, token, expirationMs);

        logger.info("User registration completed successfully for: {}", normalizedEmail);
        return response;
    }

    /**
     * Authenticates a user and generates JWT token.
     * 
     * This method handles user login by validating credentials and generating
     * a JWT token for authenticated access.
     * 
     * Process:
     * 1. Validate credentials using Spring Security
     * 2. Load user details from database
     * 3. Generate JWT token
     * 4. Update last login timestamp
     * 5. Return authentication response
     * 
     * @param loginRequest user login credentials
     * @return AuthResponse with user information and JWT token
     * @throws BadCredentialsException if credentials are invalid
     */
    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        String normalizedEmail = loginRequest.getNormalizedEmail();
        logger.info("Processing authentication for email: {}", normalizedEmail);

        try {
            // Authenticate using Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    normalizedEmail,
                    loginRequest.getPassword()
                )
            );

            // Load user details
            User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    logger.error("User not found after successful authentication: {}", normalizedEmail);
                    return new BadCredentialsException("Authentication failed");
                });

            // Note: Last login tracking can be added to User entity later
            // user.setLastLoginDate(LocalDateTime.now());
            // userRepository.save(user);

            // Generate JWT token
            String token = jwtTokenProvider.generateToken(authentication);
            long expirationMs = jwtTokenProvider.getJwtExpirationMs();

            // Convert to DTO and create response
            UserDTO userDTO = convertToUserDTO(user);
            AuthResponse response = new AuthResponse(userDTO, token, expirationMs);

            logger.info("Authentication successful for email: {}", normalizedEmail);
            return response;

        } catch (AuthenticationException e) {
            logger.warn("Authentication failed for email: {} - {}", normalizedEmail, e.getMessage());
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    /**
     * Refreshes a JWT token.
     * 
     * This method validates an existing JWT token and generates a new one
     * with extended expiration time.
     * 
     * @param token existing JWT token to refresh
     * @return RefreshTokenResponse with new token
     * @throws BadCredentialsException if token is invalid or expired
     */
    public RefreshTokenResponse refreshToken(String token) {
        logger.debug("Processing token refresh request");

        try {
            // Validate current token
            if (!jwtTokenProvider.validateToken(token)) {
                logger.warn("Token refresh failed: invalid token");
                throw new BadCredentialsException("Token is invalid or expired");
            }

            // Extract user information from token
            String username = jwtTokenProvider.getUsernameFromToken(token);
            Long userId = jwtTokenProvider.getUserIdFromToken(token);

            // Load user details to ensure user still exists and is active
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            // Verify user ID consistency
            User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            if (!user.getId().equals(userId)) {
                logger.warn("Token refresh failed: user ID mismatch for {}", username);
                throw new BadCredentialsException("Token validation failed");
            }

            // Generate new token
            String newToken = jwtTokenProvider.generateToken(user);
            long expirationMs = jwtTokenProvider.getJwtExpirationMs();

            RefreshTokenResponse response = new RefreshTokenResponse(newToken, expirationMs);
            
            logger.debug("Token refresh successful for user: {}", username);
            return response;

        } catch (Exception e) {
            logger.warn("Token refresh failed: {}", e.getMessage());
            throw new BadCredentialsException("Token refresh failed");
        }
    }

    /**
     * Handles user logout.
     * 
     * In a stateless JWT implementation, logout is primarily handled client-side
     * by discarding the token. This method can be extended to implement token
     * blacklisting or other server-side logout features.
     * 
     * @param token JWT token to logout
     * @return MessageResponse confirming logout
     */
    public MessageResponse logoutUser(String token) {
        logger.debug("Processing logout request");

        try {
            // Validate token before logout
            if (token != null && jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.getUsernameFromToken(token);
                logger.info("User logout processed for: {}", username);
                
                // Future enhancement: Add token to blacklist for server-side invalidation
                // tokenBlacklistService.blacklistToken(token);
            }

            return MessageResponse.success("Successfully logged out");

        } catch (Exception e) {
            logger.warn("Logout processing error: {}", e.getMessage());
            // Return success anyway to avoid information leakage
            return MessageResponse.success("Successfully logged out");
        }
    }

    /**
     * Checks if an email address is already registered.
     * 
     * @param email email address to check
     * @return true if email exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isEmailRegistered(String email) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : null;
        return normalizedEmail != null && userRepository.existsByEmail(normalizedEmail);
    }

    /**
     * Converts a User entity to UserDTO for API responses.
     * 
     * This method safely converts user data to a DTO that excludes
     * sensitive information like passwords.
     * 
     * @param user User entity to convert
     * @return UserDTO with safe user information
     */
    private UserDTO convertToUserDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setEmail(user.getEmail());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setRole(user.getRole());
        userDTO.setIsActive(true); // Assuming active users only can authenticate
        userDTO.setCreatedAt(user.getCreatedAt());
        userDTO.setLastLogin(null); // Last login tracking not yet implemented
        userDTO.setIsEmailVerified(false); // Default until email verification is implemented
        
        return userDTO;
    }

    /**
     * Validates that a RegisterRequest has all required fields.
     * 
     * @param request registration request to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateRegistrationRequest(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be null");
        }
        
        if (!request.hasValidRequiredFields()) {
            throw new IllegalArgumentException("All fields are required");
        }
        
        if (!request.isPasswordSecure()) {
            throw new IllegalArgumentException("Password does not meet security requirements");
        }
    }

    /**
     * Validates that a LoginRequest has required credentials.
     * 
     * @param request login request to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateLoginRequest(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request cannot be null");
        }
        
        if (!request.hasValidCredentials()) {
            throw new IllegalArgumentException("Email and password are required");
        }
        
        if (!request.hasBasicEmailFormat()) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    /**
     * Gets user information by ID for token refresh validation.
     * 
     * @param userId user ID to lookup
     * @return Optional containing User if found
     */
    @Transactional(readOnly = true)
    protected Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Gets user information by email for authentication.
     * 
     * @param email email address to lookup
     * @return Optional containing User if found
     */
    @Transactional(readOnly = true)
    protected Optional<User> getUserByEmail(String email) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : null;
        return normalizedEmail != null ? userRepository.findByEmail(normalizedEmail) : Optional.empty();
    }
}