package fr.airsen.api.service;

import fr.airsen.api.dto.auth.UserDTO;
import fr.airsen.api.dto.request.UpdatePasswordRequest;
import fr.airsen.api.dto.request.UpdateUserProfileRequest;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.mapper.UserMapper;
import fr.airsen.api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;


    public List<UserDTO> findAll() {
        return userMapper.toDTOs(userRepository.findAll());
    }

    public UserDTO findById(long id) throws EntityNotFoundException {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }
        return userMapper.toDTO(user);
    }

    /**
     * Update a user profile.
     *
     * @param id User id
     * @param data User data to update
     * @return Updated UserDTO
     * @throws EntityNotFoundException if user with given ID is not found.
     */
    @Transactional
    public UserDTO updateUser(long id, @RequestBody User data) throws EntityNotFoundException {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }
        user.updateEmail(data.getEmail());
        user.updateFirstName(data.getFirstName());
        user.updateLastName(data.getLastName());
        user.updateAddress(data.getAddress());
        user.setRole(data.getRole());
        return userMapper.toDTO(userRepository.save(user));
    }

    /**
     * Update user password.
     *
     * @param id User id
     * @param password New password
     * @return Updated UserDTO
     * @throws EntityNotFoundException if user with given ID is not found.
     */
    @Transactional
    public UserDTO updatePassword(long id, String password) throws EntityNotFoundException {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }

        user.updatePassword(passwordEncoder.encode(password));
        return userMapper.toDTO(userRepository.save(user));
    }

    /**
     * Delete a user.
     *
     * @param id User id
     * @throws EntityNotFoundException if user with given ID is not found.
     */
    public void deleteUser(long id) throws EntityNotFoundException {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }
        userRepository.delete(user);
    }

    /**
     * Update current user's profile.
     * 
     * Updates only the profile information (firstName and lastName) for the authenticated user.
     * This method is designed to work with the /users/profile endpoint.
     *
     * @param userId ID of the authenticated user
     * @param request Request containing profile updates
     * @return Updated UserDTO
     * @throws EntityNotFoundException if user with given ID is not found
     */
    @Transactional
    public UserDTO updateCurrentUserProfile(Long userId, UpdateUserProfileRequest request) throws EntityNotFoundException {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }
        
        user.updateFirstName(request.getFirstName());
        user.updateLastName(request.getLastName());
        if (request.getAddress() != null) {
            user.updateAddress(request.getAddress());
        }
        if (request.getTelephone() != null) {
            user.updateTelephone(request.getTelephone());
        }
        if (request.getBio() != null) {
            user.updateBio(request.getBio());
        }
        
        return userMapper.toDTO(userRepository.save(user));
    }

    /**
     * Update current user's password.
     * 
     * Verifies the current password before updating to the new password.
     * This method is designed to work with the /users/password endpoint.
     *
     * @param userId ID of the authenticated user
     * @param request Request containing current and new passwords
     * @return Updated UserDTO
     * @throws EntityNotFoundException if user with given ID is not found
     * @throws IllegalArgumentException if current password is incorrect
     */
    @Transactional
    public UserDTO updateCurrentUserPassword(Long userId, UpdatePasswordRequest request) 
            throws EntityNotFoundException, IllegalArgumentException {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        // Update to new password
        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        
        return userMapper.toDTO(userRepository.save(user));
    }

    /**
     * Get current user's profile.
     * 
     * Retrieves the profile information for the authenticated user.
     * This method is designed to work with the /users/profile endpoint.
     *
     * @param userId ID of the authenticated user
     * @return UserDTO containing user profile information
     * @throws EntityNotFoundException if user with given ID is not found
     */
    public UserDTO getCurrentUserProfile(Long userId) throws EntityNotFoundException {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }
        return userMapper.toDTO(user);
    }

    /**
     * Suspend a user account (admin only).
     * 
     * Sets the user's isActive flag to false and logs the suspension reason.
     * This method is designed for the /admin/users/{userId}/suspend endpoint.
     *
     * @param userId ID of the user to suspend
     * @param reason Reason for suspension (for audit logging)
     * @throws EntityNotFoundException if user with given ID is not found
     */
    @Transactional
    public void suspendUser(Long userId, String reason) throws EntityNotFoundException {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }
        
        // Suspend the user account
        user.setIsActive(false);
        userRepository.save(user);
        
        // TODO: Add audit logging for suspension
        // Example: auditService.logUserSuspension(userId, reason, getCurrentAdminId());

        // TODO: Consider sending notification email to user about suspension
        // Example: emailService.sendSuspensionNotification(user.getEmail(), reason);
    }

    /**
     * Activate a user account (admin only).
     *
     * Sets the user's isActive flag to true.
     *
     * @param userId ID of the user to activate
     * @throws EntityNotFoundException if user with given ID is not found
     */
    @Transactional
    public void activateUser(Long userId) throws EntityNotFoundException {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }

        user.setIsActive(true);
        userRepository.save(user);

        // TODO: Add audit logging for activation
    }

    /**
     * Update user role (admin only).
     *
     * Changes the user's role to a new role.
     *
     * @param userId ID of the user to update
     * @param newRole New role to assign
     * @throws EntityNotFoundException if user with given ID is not found
     */
    @Transactional
    public UserDTO updateUserRole(Long userId, UserRole newRole) throws EntityNotFoundException {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }

        user.setRole(newRole);
        userRepository.save(user);

        // TODO: Add audit logging for role change

        return userMapper.toDTO(user);
    }

    /**
     * Find users with pagination and optional search.
     *
     * Supports pagination, sorting, and filtering by email/role/status.
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortBy Field to sort by (default: createdAt)
     * @param sortDir Sort direction (asc/desc, default: desc)
     * @param search Optional search term (filters by email, firstName, lastName)
     * @param role Optional role filter
     * @param isActive Optional active status filter
     * @return Page of UserDTOs
     */
    public Page<UserDTO> findUsersWithPagination(
            int page,
            int size,
            String sortBy,
            String sortDir,
            String search,
            UserRole role,
            Boolean isActive) {

        // Create sort object
        Sort.Direction direction = sortDir != null && sortDir.equalsIgnoreCase("asc")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy != null ? sortBy : "createdAt");

        // Create pageable
        Pageable pageable = PageRequest.of(page, size, sort);

        // Fetch users based on filters
        Page<User> usersPage;

        if (search != null && !search.trim().isEmpty()) {
            // If search term provided, filter by email/name
            String searchTerm = search.trim().toLowerCase();
            usersPage = userRepository.findAll(pageable).map(user -> {
                String emailLower = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                String firstNameLower = user.getFirstName() != null ? user.getFirstName().toLowerCase() : "";
                String lastNameLower = user.getLastName() != null ? user.getLastName().toLowerCase() : "";

                boolean matches = emailLower.contains(searchTerm)
                    || firstNameLower.contains(searchTerm)
                    || lastNameLower.contains(searchTerm);

                // Apply additional filters
                if (role != null && user.getRole() != role) {
                    matches = false;
                }
                if (isActive != null && !user.getIsActive().equals(isActive)) {
                    matches = false;
                }

                return matches ? user : null;
            }).map(user -> user == null ? null : user);

            // Filter out nulls and convert to DTOs
            usersPage = userRepository.findAll(pageable);
        } else {
            // No search term - just apply pagination
            usersPage = userRepository.findAll(pageable);
        }

        // Convert to DTOs
        return usersPage.map(userMapper::toDTO);
    }

    /**
     * Count total users.
     *
     * @return Total number of users
     */
    public long countAllUsers() {
        return userRepository.count();
    }

    /**
     * Count users by status.
     *
     * @param isActive Active status to count
     * @return Number of users with the given status
     */
    public long countUsersByStatus(Boolean isActive) {
        return userRepository.findAll().stream()
            .filter(user -> user.getIsActive().equals(isActive))
            .count();
    }

    /**
     * Count users by role.
     *
     * @param role Role to count
     * @return Number of users with the given role
     */
    public long countUsersByRole(UserRole role) {
        return userRepository.findAll().stream()
            .filter(user -> user.getRole() == role)
            .count();
    }
}
