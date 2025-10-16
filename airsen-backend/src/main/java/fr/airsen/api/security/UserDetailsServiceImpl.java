package fr.airsen.api.security;

import fr.airsen.api.entity.User;
import fr.airsen.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security UserDetailsService implementation for Airsen application.
 * 
 * This service integrates the Airsen user management system with Spring Security's
 * authentication framework. It loads user information from the database during
 * authentication and converts User entities to UserPrincipal objects for
 * security context establishment.
 * 
 * Key Responsibilities:
 * - Load user details by username (email) for authentication
 * - Convert User entities to Spring Security UserDetails
 * - Handle user not found scenarios appropriately
 * - Provide transactional database access for user loading
 * - Support both login authentication and JWT token validation
 * 
 * Integration Points:
 * - Used by Spring Security during form/basic authentication
 * - Used by JwtTokenFilter for token-based authentication
 * - Used by AuthService for authentication and token refresh
 * - Integrates with existing UserRepository and User entity
 * 
 * Security Considerations:
 * - Email addresses are normalized to lowercase for consistent lookup
 * - Proper exception handling for security without information leakage
 * - Transactional operations ensure data consistency
 * - Comprehensive logging for security monitoring
 */
@Service
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * Loads user details by username (email address) for Spring Security authentication.
     * 
     * This method is called by Spring Security during authentication to retrieve
     * user information from the database. It converts the User entity to a
     * UserPrincipal that implements UserDetails for security framework integration.
     * 
     * The method performs the following operations:
     * 1. Normalize the username (email) to lowercase
     * 2. Query the database for the user by email
     * 3. Validate that the user exists and is in a valid state
     * 4. Convert the User entity to UserPrincipal
     * 5. Return UserDetails for authentication processing
     * 
     * @param username the username (email address) identifying the user to load
     * @return UserDetails implementation containing user authentication information
     * @throws UsernameNotFoundException if the user is not found or invalid
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Attempted to load user with null or empty username");
            throw new UsernameNotFoundException("Username cannot be null or empty");
        }

        // Normalize email to lowercase for consistent lookup
        String normalizedEmail = username.trim().toLowerCase();
        
        logger.debug("Loading user details for username: {}", normalizedEmail);

        try {
            // Find user by email address
            User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> {
                    logger.warn("User not found for email: {}", normalizedEmail);
                    return new UsernameNotFoundException("User not found with email: " + normalizedEmail);
                });

            // Validate user state
            if (user.getEmail() == null) {
                logger.error("User {} has null email address", user.getId());
                throw new UsernameNotFoundException("User account is in invalid state");
            }

            if (user.getRole() == null) {
                logger.error("User {} has null role", user.getEmail());
                throw new UsernameNotFoundException("User account is missing role information");
            }

            // Additional validation for password (should not be null for authentication)
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                logger.error("User {} has null or empty password hash", user.getEmail());
                throw new UsernameNotFoundException("User account is missing authentication credentials");
            }

            // Check if user account is active (not suspended)
            if (user.getIsActive() == null || !user.getIsActive()) {
                logger.warn("User {} attempted to login but account is suspended", user.getEmail());
                throw new UsernameNotFoundException("User account is suspended");
            }

            // Create UserPrincipal from User entity
            UserPrincipal userPrincipal = UserPrincipal.create(user);

            logger.debug("Successfully loaded user details for: {} with role: {}", 
                        user.getEmail(), user.getRole());

            return userPrincipal;

        } catch (UsernameNotFoundException e) {
            // Re-throw UsernameNotFoundException as-is
            throw e;
        } catch (Exception e) {
            // Log unexpected errors and convert to security exception
            logger.error("Unexpected error loading user details for: {} - {}", normalizedEmail, e.getMessage(), e);
            throw new UsernameNotFoundException("Error loading user account", e);
        }
    }

    /**
     * Loads user details by user ID for token refresh and internal operations.
     * 
     * This method provides an alternative way to load user information using
     * the user's unique identifier rather than email address. It's primarily
     * used during JWT token refresh operations where we have the user ID
     * from the token claims.
     * 
     * @param userId the unique user identifier
     * @return UserDetails implementation containing user authentication information
     * @throws UsernameNotFoundException if the user is not found or invalid
     */
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        if (userId == null) {
            logger.warn("Attempted to load user with null user ID");
            throw new UsernameNotFoundException("User ID cannot be null");
        }

        logger.debug("Loading user details for user ID: {}", userId);

        try {
            // Find user by ID
            User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("User not found for ID: {}", userId);
                    return new UsernameNotFoundException("User not found with ID: " + userId);
                });

            // Validate user state (same as loadUserByUsername)
            if (user.getEmail() == null || user.getRole() == null) {
                logger.error("User {} is in invalid state (missing email or role)", userId);
                throw new UsernameNotFoundException("User account is in invalid state");
            }

            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                logger.error("User {} has null or empty password hash", user.getEmail());
                throw new UsernameNotFoundException("User account is missing authentication credentials");
            }

            // Check if user account is active (not suspended)
            if (user.getIsActive() == null || !user.getIsActive()) {
                logger.warn("User {} (ID: {}) attempted authentication but account is suspended", user.getEmail(), userId);
                throw new UsernameNotFoundException("User account is suspended");
            }

            // Create UserPrincipal from User entity
            UserPrincipal userPrincipal = UserPrincipal.create(user);

            logger.debug("Successfully loaded user details for ID: {} (email: {}) with role: {}", 
                        userId, user.getEmail(), user.getRole());

            return userPrincipal;

        } catch (UsernameNotFoundException e) {
            // Re-throw UsernameNotFoundException as-is
            throw e;
        } catch (Exception e) {
            // Log unexpected errors and convert to security exception
            logger.error("Unexpected error loading user details for ID: {} - {}", userId, e.getMessage(), e);
            throw new UsernameNotFoundException("Error loading user account by ID", e);
        }
    }

    /**
     * Checks if a user exists by email address.
     * 
     * This method provides a way to verify user existence without loading
     * full user details, which can be useful for user registration validation
     * and other security checks.
     * 
     * @param email the email address to check
     * @return true if a user with this email exists, false otherwise
     */
    public boolean userExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase();
        
        try {
            boolean exists = userRepository.existsByEmailIgnoreCase(normalizedEmail);
            logger.debug("User existence check for {}: {}", normalizedEmail, exists);
            return exists;
        } catch (Exception e) {
            logger.error("Error checking user existence for: {} - {}", normalizedEmail, e.getMessage(), e);
            return false; // Assume user doesn't exist on error
        }
    }

    /**
     * Checks if a user exists by user ID.
     * 
     * Similar to userExists(String email) but uses the user's unique identifier.
     * 
     * @param userId the user ID to check
     * @return true if a user with this ID exists, false otherwise
     */
    public boolean userExistsById(Long userId) {
        if (userId == null) {
            return false;
        }

        try {
            boolean exists = userRepository.existsById(userId);
            logger.debug("User existence check for ID {}: {}", userId, exists);
            return exists;
        } catch (Exception e) {
            logger.error("Error checking user existence for ID: {} - {}", userId, e.getMessage(), e);
            return false; // Assume user doesn't exist on error
        }
    }

    /**
     * Gets the total number of users in the system.
     * 
     * This method can be used for administrative purposes and system monitoring.
     * 
     * @return total count of users in the database
     */
    public long getUserCount() {
        try {
            long count = userRepository.count();
            logger.debug("Total user count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting user count: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Refreshes user details from the database.
     * 
     * This method reloads user information from the database, which can be
     * useful after user profile updates or role changes to ensure the
     * security context has the most current information.
     * 
     * @param currentUserDetails the current UserDetails to refresh
     * @return fresh UserDetails loaded from the database
     * @throws UsernameNotFoundException if the user no longer exists
     */
    public UserDetails refreshUserDetails(UserDetails currentUserDetails) throws UsernameNotFoundException {
        if (currentUserDetails == null) {
            throw new UsernameNotFoundException("Cannot refresh null UserDetails");
        }

        // Use the username to reload user details
        return loadUserByUsername(currentUserDetails.getUsername());
    }
}