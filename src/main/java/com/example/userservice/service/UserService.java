package com.example.userservice.service;

import com.example.userservice.model.User;
import com.example.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public User updateUser(Long id, User userDetails) {
        return userRepository.findById(id).map(user -> {
            String newEmail = userDetails.getEmail();
            if (newEmail != null && !newEmail.equalsIgnoreCase(user.getEmail())) {
                userRepository.findByEmail(newEmail)
                        .filter(existing -> !existing.getId().equals(user.getId()))
                        .ifPresent(existing -> {
                            throw new IllegalArgumentException("Email already in use");
                        });
            }
            user.setFirstName(userDetails.getFirstName());
            user.setLastName(userDetails.getLastName());
            user.setEmail(newEmail);
            user.setAge(userDetails.getAge());
            user.setAddress(userDetails.getAddress());
            user.setJoiningDate(userDetails.getJoiningDate());
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
