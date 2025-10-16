package fr.airsen.api.mapper;

import fr.airsen.api.dto.auth.UserDTO;
import fr.airsen.api.entity.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserMapper {

    public UserDTO toDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setEmail(user.getEmail());
        userDTO.setAddress(user.getAddress());
        userDTO.setTelephone(user.getTelephone());
        userDTO.setBio(user.getBio());
        userDTO.setRole(user.getRole());
        userDTO.setIsActive(user.getIsActive());
        userDTO.setCreatedAt(user.getCreatedAt());
        userDTO.setIsEmailVerified(user.getEmailVerified());
        return userDTO;
    }

    public User toEntity(UserDTO userDTO) {
        User user = new User();
        user.setId(userDTO.getId());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setAddress(userDTO.getAddress());
        user.setTelephone(userDTO.getTelephone());
        user.setBio(userDTO.getBio());
        user.setRole(userDTO.getRole());
        user.setCreatedAt(userDTO.getCreatedAt());
        user.setEmailVerified(userDTO.getIsEmailVerified());
        user.setIsActive(userDTO.getIsActive());
        return user;
    }

    public List<UserDTO> toDTOs(List<User> users) {
        List<UserDTO> dtos = new ArrayList<>();
        for (User user : users) {
            dtos.add(toDTO(user));
        }
        return dtos;
    }

    public List<User> toEntities(List<UserDTO> userDTOs) {
        List<User> entities = new ArrayList<>();
        for (UserDTO userDTO : userDTOs) {
            entities.add(toEntity(userDTO));
        }
        return entities;
    }
}
