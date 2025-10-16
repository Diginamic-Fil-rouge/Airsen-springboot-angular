package fr.airsen.api.service;

import fr.airsen.api.dto.auth.UserDTO;
import fr.airsen.api.dto.request.UpdatePasswordRequest;
import fr.airsen.api.dto.request.UpdateUserProfileRequest;
import fr.airsen.api.entity.User;
import fr.airsen.api.mapper.UserMapper;
import fr.airsen.api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
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
}
