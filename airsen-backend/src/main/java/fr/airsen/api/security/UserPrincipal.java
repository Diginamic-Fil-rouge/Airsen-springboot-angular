package fr.airsen.api.security;

import fr.airsen.api.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security UserDetails implementation that wraps our User entity.
 * 
 * This class adapts our application's User entity to Spring Security's
 * UserDetails interface, providing the necessary user information and
 * authorities for authentication and authorization processes.
 * 
 * Key Features:
 * - Maps User entity to Spring Security UserDetails
 * - Converts UserRole enum to Spring Security authorities
 * - Provides account status management
 * - Enables role-based authorization throughout the application
 * 
 * Integration Points:
 * - Used by UserDetailsService to load user information
 * - Consumed by JwtTokenFilter for authentication context
 * - Enables @PreAuthorize annotations on controllers
 */
public class UserPrincipal implements UserDetails {

    private final User user;

    /**
     * Creates a UserPrincipal wrapping the given User entity.
     * 
     * @param user the User entity to wrap
     * @throws IllegalArgumentException if user is null
     */
    public UserPrincipal(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        this.user = user;
    }

    /**
     * Returns the authorities granted to the user.
     * 
     * Maps the User's role to Spring Security authorities with the
     * standard "ROLE_" prefix. This enables role-based authorization
     * using @PreAuthorize annotations and security expressions.
     * 
     * @return collection containing the user's authority based on their role
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user.getRole() == null) {
            return Collections.emptyList();
        }
        
        String roleName = "ROLE_" + user.getRole().name();
        return Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }

    /**
     * Returns the password used to authenticate the user.
     * 
     * @return the user's hashed password
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * Returns the username used to authenticate the user.
     * 
     * In our application, we use the email address as the username
     * for authentication purposes.
     * 
     * @return the user's email address
     */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    /**
     * Indicates whether the user's account has expired.
     * 
     * An expired account cannot be authenticated. In our current
     * implementation, accounts do not expire automatically.
     * 
     * @return true if the user's account is valid (non-expired)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is locked or unlocked.
     * 
     * A locked user cannot be authenticated. This could be used
     * for implementing account lockout policies in the future.
     * 
     * @return true if the user is not locked
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials (password) has expired.
     * 
     * Expired credentials prevent authentication. This could be used
     * for implementing password expiration policies in the future.
     * 
     * @return true if the user's credentials are valid (non-expired)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled or disabled.
     * 
     * A disabled user cannot be authenticated. This provides a way
     * to temporarily disable user accounts without deleting them.
     * 
     * @return true if the user is enabled
     */
    @Override
    public boolean isEnabled() {
        // You can add custom logic here if User entity has an 'enabled' field
        // For now, all users are considered enabled
        return true;
    }

    /**
     * Returns the wrapped User entity.
     * 
     * This provides access to additional user properties that are not
     * part of the UserDetails interface, such as first name, last name,
     * creation date, etc.
     * 
     * @return the wrapped User entity
     */
    public User getUser() {
        return user;
    }

    /**
     * Returns the user's unique identifier.
     * 
     * Convenience method to access the user's ID without unwrapping
     * the User entity.
     * 
     * @return the user's unique identifier
     */
    public Long getId() {
        return user.getId();
    }

    /**
     * Returns the user's email address.
     * 
     * Convenience method that delegates to getUsername() for consistency
     * with our authentication model.
     * 
     * @return the user's email address
     */
    public String getEmail() {
        return user.getEmail();
    }

    /**
     * Returns the user's first name.
     * 
     * @return the user's first name
     */
    public String getFirstName() {
        return user.getFirstName();
    }

    /**
     * Returns the user's last name.
     * 
     * @return the user's last name
     */
    public String getLastName() {
        return user.getLastName();
    }

    /**
     * Returns the user's role.
     * 
     * @return the user's role enum
     */
    public fr.airsen.api.entity.enums.UserRole getRole() {
        return user.getRole();
    }

    /**
     * Checks if the user has a specific role.
     * 
     * Convenience method for role-based logic in services and controllers.
     * 
     * @param role the role to check
     * @return true if the user has the specified role
     */
    public boolean hasRole(fr.airsen.api.entity.enums.UserRole role) {
        return user.getRole() == role;
    }

    /**
     * Returns a string representation of this UserPrincipal.
     * 
     * Includes the username and role for debugging purposes.
     * Does not include sensitive information like passwords.
     * 
     * @return string representation of the user principal
     */
    @Override
    public String toString() {
        return "UserPrincipal{" +
                "username='" + getUsername() + '\'' +
                ", role=" + getRole() +
                ", enabled=" + isEnabled() +
                '}';
    }

    /**
     * Creates a UserPrincipal from a User entity.
     * 
     * Factory method for convenient UserPrincipal creation.
     * 
     * @param user the User entity to wrap
     * @return a new UserPrincipal instance
     * @throws IllegalArgumentException if user is null
     */
    public static UserPrincipal create(User user) {
        return new UserPrincipal(user);
    }
}