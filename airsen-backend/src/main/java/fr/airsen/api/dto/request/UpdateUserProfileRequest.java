package fr.airsen.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating user profile information.
 * 
 * This DTO represents the data that can be updated by users in their profile.
 * It only includes fields that are safe for users to modify themselves.
 * 
 * Validation:
 * - First name and last name are required and must be non-empty
 * - Names must be between 1 and 50 characters
 * 
 * Security Considerations:
 * - Only includes fields that users should be able to modify
 * - Excludes sensitive fields like email, role, or account status
 * - Does NOT include external API data (air quality, weather, INSEE)
 * - These changes only affect the authenticated user's own profile
 */
public class UpdateUserProfileRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;

    @Size(max = 200, message = "Address must not exceed 200 characters")
    private String address;

    @Size(max = 20, message = "Telephone must not exceed 20 characters")
    private String telephone;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    public UpdateUserProfileRequest() {
    }

    public UpdateUserProfileRequest(String firstName, String lastName, String address, String telephone, String bio) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.telephone = telephone;
        this.bio = bio;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    @Override
    public String toString() {
        return "UpdateUserProfileRequest{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", address='" + address + '\'' +
                ", telephone='" + telephone + '\'' +
                ", bio='" + bio + '\'' +
                '}';
    }
}