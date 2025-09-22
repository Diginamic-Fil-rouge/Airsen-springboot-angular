package fr.airsen.api.service;

import fr.airsen.api.dto.auth.UserDTO;
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

    @Transactional
    public UserDTO updatePassword(long id, String password) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }

        user.updatePassword(passwordEncoder.encode(password));
        return userMapper.toDTO(userRepository.save(user));
    }

    public void deleteUser(long id) throws EntityNotFoundException {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }
        userRepository.delete(user);
    }
}
