package com.milosz.podsiadly.domain.bank.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BankDto(
        Long id,
        @NotBlank(message = "Bank name cannot be empty")
        @Size(max = 100, message = "The bank name can have maximum of 100 characters")
        String name,
        @NotBlank(message = "Bank BIC cannot be blank")
        @Size(min = 8, max = 11, message = "BIC must have from 8 to 11 characters")
        String bic,
        String address,
        @Email(message = "Incorrect email address format")
        String contactEmail
) {}
