package com.sms.dto;

public record LoginResponse(String token, String tokenType, long userId, String username,
                            String fullName, String role) {}
