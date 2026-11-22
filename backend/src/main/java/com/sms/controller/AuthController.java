package com.sms.controller;

import com.sms.dto.ChangePasswordRequest;
import com.sms.dto.LoginRequest;
import com.sms.dto.RegisterRequest;
import com.sms.model.AppUser;
import com.sms.model.Role;
import com.sms.dto.LoginResponse;
import com.sms.dto.UserResponse;
import com.sms.security.JwtService;
import com.sms.service.CurrentUserService;
import com.sms.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserRepository users;
    private final JwtService jwtService;
    private final CurrentUserService currentUser;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, UserRepository users,
                          JwtService jwtService, CurrentUserService currentUser, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager; this.users = users;
        this.jwtService = jwtService; this.currentUser = currentUser; this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        var user = users.findByUsername(request.username())
                .or(() -> users.findByEmail(request.username()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!user.isActive()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is inactive");
        return new LoginResponse(jwtService.generateToken(user), "Bearer", user.getId(),
                user.getUsername(), user.getFullName(), user.getRole().name());
    }


    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public java.util.Map<String, String> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        String username = email;
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This email is already registered");
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.STUDENT);
        user.setActive(true);
        users.save(user);
        return java.util.Map.of("message", "Registration successful. You can now log in.");
    }

    @PutMapping("/password")
    public java.util.Map<String, String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        var user = currentUser.requireCurrentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        users.save(user);
        return java.util.Map.of("message", "Password changed successfully");
    }

    @GetMapping("/me")
    public UserResponse me() {
        return UserResponse.from(currentUser.requireCurrentUser());
    }
}
