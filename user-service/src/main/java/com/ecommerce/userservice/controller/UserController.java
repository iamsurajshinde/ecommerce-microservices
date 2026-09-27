package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.LoginRequest;
import com.ecommerce.userservice.dto.LoginResponse;
import com.ecommerce.userservice.dto.UserResponse;
import com.ecommerce.userservice.dto.UpdateProfileRequest;
import com.ecommerce.userservice.dto.UpdateRoleRequest;
import com.ecommerce.userservice.model.User;
import com.ecommerce.userservice.service.JwtService;
import com.ecommerce.userservice.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        User savedUser = userService.saveUser(user);
        return ResponseEntity.ok(toUserResponse(savedUser));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        User user = userService.authenticate(request.email(), request.password());
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(new LoginResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getRole()
        ));
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return user == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(toUserResponse(user));
    }

    @PutMapping("/{id}/profile")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponse> updateProfile(
            @PathVariable Long id,
            @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        User currentUser = userService.getUserById(id);
        if (currentUser == null
                || authentication == null
                || !currentUser.getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You can update only your own profile.");
        }

        return ResponseEntity.ok(toUserResponse(userService.updateProfile(id, request)));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/role")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponse> updateRole(
            @PathVariable Long id, @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(toUserResponse(userService.updateRole(id, request)));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}