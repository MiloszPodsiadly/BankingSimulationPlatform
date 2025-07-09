package com.milosz.podsiadly.common.validation;

import com.milosz.podsiadly.domain.user.repository.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component; // Oznacza jako komponent Springa

@Component // Musi być komponentem, aby wstrzykiwać zależności
@RequiredArgsConstructor
public class UniqueEmailValidatorImpl implements ConstraintValidator<UniqueEmailValidator, String> {

    private final UserRepository userRepository; // Wstrzykujemy UserRepository

    @Override
    public void initialize(UniqueEmailValidator constraintAnnotation) {
        // Inicjalizacja, jeśli potrzebna
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.trim().isEmpty()) {
            return true; // Null or empty emails are handled by @NotBlank/@NotNull
        }
        return !userRepository.existsByEmail(email); // Sprawdzamy, czy email już istnieje
    }
}