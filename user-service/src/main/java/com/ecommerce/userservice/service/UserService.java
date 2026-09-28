package com.ecommerce.userservice.service;

import com.ecommerce.userservice.exception.InvalidCredentialsException;
import com.ecommerce.userservice.dto.UpdateProfileRequest;
import com.ecommerce.userservice.dto.UpdateRoleRequest;
import com.ecommerce.userservice.event.EventPublisher;
import com.ecommerce.userservice.model.User;
import com.ecommerce.userservice.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;
    private final EventPublisher eventPublisher;

    public UserService(
            UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder,
            CacheManager cacheManager,
            EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cacheManager = cacheManager;
        this.eventPublisher = eventPublisher;
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
        User savedUser = userRepository.save(user);
        publishUserRegistered(savedUser);
        return savedUser;
    }

    private void publishUserRegistered(User user) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", "user-registered-" + user.getId() + "-" + UUID.randomUUID());
        payload.put("userId", user.getId());
        payload.put("email", user.getEmail());
        payload.put("name", user.getName());
        eventPublisher.publish(EventPublisher.RK_USER_REGISTERED, payload);
    }

    public boolean existsByEmail(String email) {
        return email != null && userRepository.existsByEmailIgnoreCase(normalizeEmail(email));
    }

    public User authenticate(String email, String password) {
        if (email == null || password == null) {
            throw new InvalidCredentialsException();
        }

        User user = getUserByEmail(email);
        if (user == null) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return user;
    }

    @Cacheable(
            cacheNames = "usersByEmail",
            key = "#email",
            unless = "#result == null")
    public User getUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email)).orElse(null);
    }

    @Cacheable(
            cacheNames = "usersById",
            key = "#id")
    public User getUserById(Long id) {
        log.debug("User cache miss for id={}; loading user from database", id);
        return userRepository.findById(id).orElseThrow(()->new IllegalArgumentException("User not found."));
    }

    @CacheEvict(cacheNames = "usersById", key = "#id")
    public User updateProfile(Long id, UpdateProfileRequest request) {
        User user = this.getUserById(id);
        String previousEmail = user.getEmail();

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

        User updatedUser = userRepository.save(user);
        evictEmailCache(previousEmail);
        evictEmailCache(updatedUser.getEmail());
        return updatedUser;
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank.");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Email must be a valid email address.");
        }
    }

    public String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @CacheEvict(cacheNames = "usersById", key = "#id")
    public User updateRole(Long id, UpdateRoleRequest request) {
        if (request == null || request.role() == null || request.role().isBlank()) {
            throw new IllegalArgumentException("Role is required.");
        }
        String role = request.role().trim().toUpperCase();
        if (!role.equals("USER") && !role.equals("ADMIN")) {
            throw new IllegalArgumentException("Role must be USER or ADMIN.");
        }
        User user = this.getUserById(id);
        user.setRole(role);
        User updatedUser = userRepository.save(user);
        evictEmailCache(updatedUser.getEmail());
        return updatedUser;
    }

    private void evictEmailCache(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        Cache cache = cacheManager.getCache("usersByEmail");
        if (cache != null) {
            cache.evict(normalizeEmail(email));
        }
    }
}