package com.sms.controller;

import com.sms.model.AppUser;
import com.sms.repository.UserRepository;
import com.sms.service.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final CurrentUserService current; private final UserRepository users;
    public ProfileController(CurrentUserService current,UserRepository users){this.current=current;this.users=users;}
    public record ProfileUpdate(@NotBlank @Size(max=120) String fullName,@NotBlank @Email String email) {}
    public record ProfileView(Long id,String username,String email,String fullName,String role,boolean active) {
        static ProfileView from(AppUser u){return new ProfileView(u.getId(),u.getUsername(),u.getEmail(),u.getFullName(),u.getRole().name(),u.isActive());}
    }
    @GetMapping("/me") public ProfileView get(){return ProfileView.from(current.requireCurrentUser());}
    @PutMapping("/me") public ProfileView update(@Valid @RequestBody ProfileUpdate req){
        AppUser u=current.requireCurrentUser();String email=req.email().trim().toLowerCase(Locale.ROOT);
        if(users.findByEmail(email).filter(other->!other.getId().equals(u.getId())).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email is already in use");
        if(users.findByUsername(email).filter(other->!other.getId().equals(u.getId())).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email is already used as a username");
        u.setFullName(req.fullName().trim());u.setEmail(email);
        // Registered accounts use email as username; keep login aliases synchronized when applicable.
        if(u.getUsername().contains("@")) u.setUsername(email);
        return ProfileView.from(users.save(u));
    }
}
