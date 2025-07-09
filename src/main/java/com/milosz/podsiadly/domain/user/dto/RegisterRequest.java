package com.milosz.podsiadly.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.milosz.podsiadly.domain.user.model.Role.UserRole;
import com.milosz.podsiadly.common.validation.ValidPassword; // Import nowej adnotacji
import com.milosz.podsiadly.common.validation.UniqueEmailValidator; // Import nowej adnotacji

import java.util.Set;

public record RegisterRequest(
        @NotBlank(message = "Username cannot be blank")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Password cannot be blank")
        @ValidPassword // Dodajemy adnotację do walidacji złożoności hasła
        String password,

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email should be valid")
        @UniqueEmailValidator // Dodajemy adnotację do walidacji unikalności emaila
        String email,

        @NotBlank(message = "First name cannot be blank")
        String firstName,

        @NotBlank(message = "Last name cannot be blank")
        String lastName,

        Set<UserRole> roles
) {}