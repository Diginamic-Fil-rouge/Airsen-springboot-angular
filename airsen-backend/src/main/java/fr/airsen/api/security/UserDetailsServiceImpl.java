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
 * Spring Security UserDetailsService implementation for loading user data.
 * 
 * This service integrates our application's User entity with Spring Security's
 * authentication framework. It loads user information from the database and
 * adapts it to Spring Security's UserDetails interface through UserPrincipal.
 * 
 * Key Responsibilities:
 * - Load user data by username (email) from the database
 * - Convert User entity to UserDetails (via UserPrincipal)
 * - Handle user not found scenarios
 * - Support Spring Security's authentication process
 * - Enable JWT-based stateless authentication
 * 
 * Integration Points:
 * - Called by Spring Security during authentication
 * - Used by JwtTokenFilter for token-based authentication
 * - Supports both form-based and JWT authentication flows
 * - Enables role-based authorization across the application
 */
@Service
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * Loads user details by username for Spring Security authentication.
     * 
     * This method is called by Spring Security's authentication framework
     * to retrieve user information during the authentication process.
     * In our application, the username is the user's email address.
     * 
     * Authentication Flow:
     * 1. User submits credentials (login form) or JWT token is validated
     * 2. Spring Security calls this method with the username
     * 3. We query the database using UserRepository
     * 4. Convert User entity to UserPrincipal (UserDetails)
     * 5. Spring Security uses the returned UserDetails for authorization
     * 
     * @param username the email address of the user to authenticate
     * @return UserDetails object containing user information and authorities
     * @throws UsernameNotFoundException if no user found with the given email
     * @throws IllegalArgumentException if username is null or empty
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);
        
        // Validate input parameters
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Attempted to load user with null or empty username");
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        
        // Normalize username (email) to lowercase for case-insensitive lookup
        String normalizedEmail = username.trim().toLowerCase();
        
        try {
            // Query database for user by email
            User user = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> {
                        logger.warn("User not found with email: {}", normalizedEmail);
                        return new UsernameNotFoundException(
                            "User not found with email: " + normalizedEmail
                        );
                    });
            
            // Validate user data integrity
            if (user.getRole() == null) {
                logger.error("User {} has null role, authentication denied", normalizedEmail);
                throw new UsernameNotFoundException(
                    "User account is invalid: missing role information"
                );
            }
            
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                logger.error("User {} has null or empty password, authentication denied", normalizedEmail);
                throw new UsernameNotFoundException(
                    "User account is invalid: missing password information"
                );
            }
            
            logger.debug("Successfully loaded user: {} with role: {}", 
                        normalizedEmail, user.getRole());
            
            // Create and return UserPrincipal wrapping the User entity
            return UserPrincipal.create(user);
            
        } catch (UsernameNotFoundException ex) {
            // Re-throw UsernameNotFoundException as-is
            throw ex;
        } catch (Exception ex) {
            // Log unexpected database errors
            logger.error("Unexpected error loading user by username: {}", normalizedEmail, ex);
            throw new UsernameNotFoundException(
                "Unable to load user due to system error", ex
            );
        }
    }

    /**
     * Loads user details by user ID.
     * 
     * This method is useful for JWT token authentication where we have
     * the user ID from the token and need to load the full user details.
     * This is an extension to the standard UserDetailsService interface.
     * 
     * @param userId the unique identifier of the user
     * @return UserDetails object containing user information and authorities
     * @throws UsernameNotFoundException if no user found with the given ID
     * @throws IllegalArgumentException if userId is null
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        logger.debug("Loading user by ID: {}", userId);
        
        // Validate input parameters
        if (userId == null) {
            logger.warn("Attempted to load user with null ID");
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        try {
            // Query database for user by ID
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        logger.warn("User not found with ID: {}", userId);
                        return new UsernameNotFoundException(
                            "User not found with ID: " + userId
                        );
                    });
            
            // Validate user data integrity
            if (user.getRole() == null) {
                logger.error("User with ID {} has null role, authentication denied", userId);
                throw new UsernameNotFoundException(
                    "User account is invalid: missing role information"
                );
            }
            
            logger.debug("Successfully loaded user by ID: {} (email: {}) with role: {}", 
                        userId, user.getEmail(), user.getRole());
            
            // Create and return UserPrincipal wrapping the User entity
            return UserPrincipal.create(user);
            
        } catch (UsernameNotFoundException ex) {
            // Re-throw UsernameNotFoundException as-is
            throw ex;
        } catch (Exception ex) {
            // Log unexpected database errors
            logger.error("Unexpected error loading user by ID: {}", userId, ex);
            throw new UsernameNotFoundException(
                "Unable to load user due to system error", ex
            );
        }
    }

    /**
     * Checks if a user exists with the given email address.
     * 
     * Utility method for registration and validation processes.
     * 
     * @param email the email address to check
     * @return true if a user exists with the given email
     * @throws IllegalArgumentException if email is null or empty
     */
    @Transactional(readOnly = true)
    public boolean userExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        String normalizedEmail = email.trim().toLowerCase();
        boolean exists = userRepository.existsByEmail(normalizedEmail);
        
        logger.debug("User existence check for email {}: {}", normalizedEmail, exists);
        return exists;
    }

    /**
     * Validates that a user account is active and can be authenticated.
     * 
     * This method performs additional validation beyond the standard
     * UserDetailsService contract. It can be used to implement custom
     * business rules for user authentication.
     * 
     * @param user the User entity to validate
     * @return true if the user account is valid for authentication
     */
    private boolean isUserAccountValid(User user) {
        if (user == null) {
            return false;
        }
        
        // Check for required fields
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            logger.warn("User {} has invalid email", user.getId());
            return false;
        }
        
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            logger.warn("User {} has invalid password", user.getId());
            return false;
        }
        
        if (user.getRole() == null) {
            logger.warn("User {} has invalid role", user.getId());
            return false;
        }
        
        // Additional business rules can be added here
        // For example: account expiration, suspension, etc.
        
        return true;
    }

    /**
     * Returns the UserRepository used by this service.
     * 
     * Exposed for testing purposes and potential customization.
     * 
     * @return the UserRepository instance
     */
    protected UserRepository getUserRepository() {
        return userRepository;
    }
}