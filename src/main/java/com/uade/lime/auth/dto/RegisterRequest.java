package com.uade.lime.auth.dto;

import java.time.LocalDate;

import com.uade.lime.auth.model.Sex;
import com.uade.lime.auth.model.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String name,
        UserRole role,
        @NotNull @Past LocalDate birthDate,
        @NotNull Sex sex,
        @Size(max = 150) String agencyName) {
}
