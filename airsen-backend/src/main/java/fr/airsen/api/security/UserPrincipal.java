package fr.airsen.api.security;

import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Spring Security UserDetails implementation for Airsen users.
 * 
 * This class adapts the Airsen User entity to Spring Security's UserDetails interface,
 * providing the necessary authentication and authorization information for the security
 * framework. It encapsulates user credentials, authorities, and account status.
 * 
 * Key Features:
 * - Implements Spring Security UserDetails interface
 * - Maps Airsen user roles to Spring Security authorities
 * - Provides account status information (enabled, expired, locked, etc.)
 * - Encapsulates user identity and authentication data
 * - Thread-safe and immutable after construction
 * 
 * Authority Mapping:
 * - VISITOR -> ROLE_VISITOR
 * - USER -> ROLE_USER  
 * - ADMIN -> ROLE_ADMIN
 * 
 * Account Status:
 * - All accounts are considered enabled and non-expired by default
 * - Password expiration and account locking can be extended in future versions
 */
public class UserPrincipal implements UserDetails {

    private final Long id;

    private final String email;

    private final String password;

    private final String firstName;

    private final String lastName;

    private final UserRole role;

    private final boolean enabled;

    /**
     * Spring Security authorities based on user role.
     */
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Private constructor to enforce factory method usage.
     * 
     * @param id user ID
     * @param email user email
     * @param password user password hash
     * @param firstName user first name
     * @param lastName user last name
     * @param role user role
     * @param enabled whether account is enabled
     * @param authorities Spring Security authorities
     */
    private UserPrincipal(Long id, String email, String password, String firstName, 
                         String lastName, UserRole role, boolean enabled,
                         Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    /**
     * Creates a UserPrincipal from an Airsen User entity.
     * 
     * This factory method converts a User entity to a UserPrincipal,
     * mapping the user role to appropriate Spring Security authorities.
     * 
     * @param user User entity to convert
     * @return UserPrincipal for Spring Security authentication
     * @throws IllegalArgumentException if user is null or invalid
     */
    public static UserPrincipal create(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("User email cannot be null or empty");
        }
        
        if (user.getRole() == null) {
            throw new IllegalArgumentException("User role cannot be null");
        }

        // Map user role to Spring Security authority
        Collection<GrantedAuthority> authorities = Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return new UserPrincipal(
            user.getId(),
            user.getEmail(),
            user.getPassword(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole(),
            true, // All users are considered enabled by default
            authorities
        );
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public UserRole getRole() {
        return role;
    }

    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return email;
        }
    }

    // Spring Security UserDetails interface methods

    /**
     * Returns the authorities granted to the user.
     * 
     * Maps Airsen user roles to Spring Security authorities:
     * - VISITOR -> ROLE_VISITOR
     * - USER -> ROLE_USER
     * - ADMIN -> ROLE_ADMIN
     * 
     * @return collection of granted authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Returns the user's password hash.
     * 
     * Used by Spring Security for authentication verification.
     * 
     * @return hashed password
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Returns the username used to authenticate the user.
     * 
     * In Airsen, the email address serves as the username.
     * 
     * @return user email address
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Indicates whether the user's account has expired.
     * 
     * Currently, all Airsen accounts are considered non-expired.
     * This can be extended in future versions for account lifecycle management.
     * 
     * @return true (account never expires)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is locked or unlocked.
     * 
     * Currently, Airsen accounts are never locked.
     * This can be extended for security features like account lockout
     * after failed authentication attempts.
     * 
     * @return true (account never locked)
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials (password) has expired.
     * 
     * Currently, Airsen passwords never expire.
     * This can be extended for password rotation policies.
     * 
     * @return true (credentials never expire)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled or disabled.
     * 
     * A disabled user cannot be authenticated.
     * 
     * @return true if user is enabled
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // Object contract methods

    /**
     * Compares this UserPrincipal to another object for equality.
     * 
     * Two UserPrincipals are equal if they have the same ID and email.
     * 
     * @param o object to compare with
     * @return true if objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        UserPrincipal that = (UserPrincipal) o;
        return Objects.equals(id, that.id) && 
               Objects.equals(email, that.email);
    }

    /**
     * Returns hash code for this UserPrincipal.
     * 
     * Based on ID and email for consistency with equals method.
     * 
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    /**
     * Returns string representation of this UserPrincipal.
     * 
     * Excludes password for security reasons.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        return "UserPrincipal{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", role=" + role +
                ", enabled=" + enabled +
                ", authorities=" + authorities +
                '}';
    }
}