package fr.airsen.api.dto;

import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Forum author information")
public class ForumAuthorDTO {

    @Schema(description = "User ID", example = "123")
    private Long id;

    @Schema(description = "First name", example = "Jane")
    private String firstName;

    @Schema(description = "Last name", example = "Lolny")
    private String lastName;

    @Schema(description = "User role", example = "USER")
    private UserRole role;

    public ForumAuthorDTO() {
    }

    /**
     * Constructs ForumAuthorDTO from User entity.
     * Extracts only public-safe fields needed for forum displays.
     *
     * @param user the user entity
     */
    public ForumAuthorDTO(User user) {
        if (user != null) {
            this.id = user.getId();
            this.firstName = user.getFirstName();
            this.lastName = user.getLastName();
            this.role = user.getRole();
        }
    }

    /**
     * Full constructor for manual creation.
     *
     * @param id user ID
     * @param firstName user first name
     * @param lastName user last name
     * @param role user role
     */
    public ForumAuthorDTO(Long id, String firstName, String lastName, UserRole role) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return "Unknown";
        }
    }

    /**
     * Gets user initials for avatar display.
     * Useful for UI components that show user avatars.
     *
     * @return initials (e.g., "JD" for John Doe) or "?" if no names available
     */
    public String getInitials() {
        StringBuilder initials = new StringBuilder();

        if (firstName != null && !firstName.isEmpty()) {
            initials.append(firstName.charAt(0));
        }

        if (lastName != null && !lastName.isEmpty()) {
            initials.append(lastName.charAt(0));
        }

        return initials.length() > 0 ? initials.toString().toUpperCase() : "?";
    }

    @Override
    public String toString() {
        return "ForumAuthorDTO{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", role=" + role +
                '}';
    }
}
