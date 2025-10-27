package fr.airsen.api.service;

import fr.airsen.api.entity.User;
import fr.airsen.api.repository.UserRepository;
import fr.airsen.api.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service to get the currently authenticated user.
 *
 * This service provides an easy way for controllers to access the full User entity
 * of the authenticated user. It extracts the UserPrincipal from Spring Security's
 * context and loads the complete User entity from the database.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Gets the currently authenticated user as a User entity.
     *
     * This method:
     * 1. Gets the authentication from Spring Security context
     * 2. Extracts the UserPrincipal (which has user ID, email, role)
     * 3. Loads the full User entity from database
     *
     * @return the authenticated User entity
     * @throws IllegalStateException if user is not authenticated or not found
     */
    public User getCurrentUser() {
        // Get authentication from Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        // Extract UserPrincipal from authentication
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal)) {
            throw new IllegalStateException("Authentication principal is not a UserPrincipal");
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;

        // Load full User entity from database using the ID from UserPrincipal
        return userRepository.findById(userPrincipal.getId())
            .orElseThrow(() -> new IllegalStateException(
                "User not found with ID: " + userPrincipal.getId()));
    }

    /**
     * Gets the currently authenticated user's ID.
     * This is faster than getCurrentUser() if you only need the ID.
     *
     * @return the authenticated user's ID
     * @throws IllegalStateException if user is not authenticated
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal)) {
            throw new IllegalStateException("Authentication principal is not a UserPrincipal");
        }

        return ((UserPrincipal) principal).getId();
    }
}
