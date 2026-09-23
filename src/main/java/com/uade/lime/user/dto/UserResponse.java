package com.uade.lime.user.dto;

import com.uade.lime.auth.model.User;

public record UserResponse(
        Long id,
        String email,
        String name,
        String role,
        String agencyName) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getAgencyName());
    }
}
