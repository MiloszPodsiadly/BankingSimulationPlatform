package com.milosz.podsiadly.domain.user.dto;

import com.milosz.podsiadly.domain.user.model.Role.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Set;

public record UserDto(
        Long id,
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull Set<UserRole> roles, // Now a Set of UserRole
        boolean active,
        LocalDateTime registrationDate,
        LocalDateTime lastLoginDate
) {}