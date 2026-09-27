package com.ecommerce.userservice.service;

import com.ecommerce.userservice.exception.InvalidCredentialsException;
import com.ecommerce.userservice.dto.UpdateProfileRequest;
import com.ecommerce.userservice.dto.UpdateRoleRequest;
import com.ecommerce.userservice.model.User;
import com.ecommerce.userservice.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User saveUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User details are required.");
        }
        validateEmail(user.getEmail());
        String normalizedEmail = normalizeEmail(user.getEmail());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalStateException("Email is already in use.");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password must not be blank.");
        }
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public boolean existsByEmail(String email) {
        return email != null && userRepository.existsByEmailIgnoreCase(normalizeEmail(email));
    }

    public User authenticate(String email, String password) {
        if (email == null || password == null) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
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
        if (request.email() != null) {
            validateEmail(request.email());
        }
        if (request.password() != null && request.password().isBlank()) {
            throw new IllegalArgumentException("Password must not be blank.");
        }
        if (request.email() != null && !normalizeEmail(request.email()).equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmailIgnoreCase(normalizeEmail(request.email()))) {
            throw new IllegalStateException("Email is already in use.");
        }

        if (request.name() != null) {
            user.setName(request.name().trim());
        }
        if (request.email() != null) {
            user.setEmail(normalizeEmail(request.email()));
        }
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        return userRepository.save(user);
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank.");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Email must be a valid email address.");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public User updateRole(Long id, UpdateRoleRequest request) {
        if (request == null || request.role() == null || request.role().isBlank()) {
            throw new IllegalArgumentException("Role is required.");
        }
        String role = request.role().trim().toUpperCase();
        if (!role.equals("USER") && !role.equals("ADMIN")) {
            throw new IllegalArgumentException("Role must be USER or ADMIN.");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        user.setRole(role);
        return userRepository.save(user);
    }
}