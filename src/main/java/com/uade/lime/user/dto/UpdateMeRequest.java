package com.uade.lime.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateMeRequest(
        @Size(max = 100) String name,
        @Size(max = 100) String agencyName,
        String currentPassword,
        @Size(min = 8, max = 72) String newPassword) {
}
