package fr.airsen.api.controller;

import fr.airsen.api.dto.auth.UserDTO;
import fr.airsen.api.entity.User;
import fr.airsen.api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public List<UserDTO> getAllUsers(){
        return userService.findAll();
    }

    @GetMapping("/profile/{id}")
    public UserDTO getUser(@PathVariable String id){
        return userService.findById(Long.parseLong(id));
    }

    @PutMapping("/profile/{id}")
    public UserDTO updateUser(@PathVariable String id, @RequestBody User user){
        return userService.updateUser(Long.parseLong(id), user);
    }

    @PutMapping("/password/{id}")
    public UserDTO updatePassword(@PathVariable String id, @RequestBody String password){
        return userService.updatePassword(Long.parseLong(id), password);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable String id){
        userService.deleteUser(Long.parseLong(id));
    }
}
