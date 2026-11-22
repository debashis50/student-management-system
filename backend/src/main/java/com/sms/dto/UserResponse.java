package com.sms.dto;

import com.sms.model.AppUser;

public record UserResponse(Long id, String username, String email, String fullName, String role, boolean active) {
    public static UserResponse from(AppUser u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(), u.getRole().name(), u.isActive());
    }
}
