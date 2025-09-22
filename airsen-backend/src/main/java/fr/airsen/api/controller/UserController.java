package fr.airsen.api.controller;

import fr.airsen.api.dto.auth.UserDTO;
import fr.airsen.api.entity.User;
import fr.airsen.api.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User Controller.
 *
 * Controller for managing user endpoints.
 */
@RestController
@RequestMapping("/users")
@Tag(name = "User Controller", description = "User management endpoints")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Get all users.
     *
     * @return List of UserDTO
     */
    @GetMapping
    public List<UserDTO> getAllUsers(){
        return userService.findAll();
    }

    /**
     * Get user by id.
     *
     * @param id User id
     * @return UserDTO
     */
    @GetMapping("/profile/{id}")
    public UserDTO getUser(@PathVariable String id){
        return userService.findById(Long.parseLong(id));
    }

    /**
     * Update a user profile.
     *
     * @param id User id
     * @param user User to update
     * @return UserDTO
     */
    @PutMapping("/profile/{id}")
    public UserDTO updateUser(@PathVariable String id, @RequestBody User user){
        return userService.updateUser(Long.parseLong(id), user);
    }

    /**
     * Update user password.
     *
     * @param id User id
     * @param password New password
     * @return UserDTO
     */
    @PutMapping("/password/{id}")
    public UserDTO updatePassword(@PathVariable String id, @RequestBody String password){
        return userService.updatePassword(Long.parseLong(id), password);
    }

    /**
     * Delete a user.
     *
     * @param id User id
     */
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable String id){
        userService.deleteUser(Long.parseLong(id));
    }
}