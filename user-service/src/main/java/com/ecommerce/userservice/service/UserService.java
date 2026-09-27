package com.ecommerce.userservice.service;

import com.ecommerce.userservice.exception.InvalidCredentialsException;
import com.ecommerce.userservice.dto.UpdateProfileRequest;
import com.ecommerce.userservice.model.User;
import com.ecommerce.userservice.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User authenticate(String email, String password) {
        if (email == null || password == null) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return user;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User updateProfile(Long id, UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (request == null) {
            throw new IllegalArgumentException("Profile update request is required.");
        }
        if (request.name() != null && request.name().isBlank()) {
            throw new IllegalArgumentException("Name must not be blank.");
        }
        if (request.email() != null && request.email().isBlank()) {
            throw new IllegalArgumentException("Email must not be blank.");
        }
        if (request.password() != null && request.password().isBlank()) {
            throw new IllegalArgumentException("Password must not be blank.");
        }
        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email is already in use.");
        }

        if (request.name() != null) {
            user.setName(request.name().trim());
        }
        if (request.email() != null) {
            user.setEmail(request.email().trim());
        }
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        return userRepository.save(user);
    }
}