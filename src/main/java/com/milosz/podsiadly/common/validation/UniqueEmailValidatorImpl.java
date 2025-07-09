package com.milosz.podsiadly.common.validation;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import com.milosz.podsiadly.domain.user.repository.UserRepository; // Będzie dodane w następnym kroku

/**
 * Walidator sprawdzający unikalność adresu e-mail w bazie danych.
 * Wymaga dostępu do UserRepository. Pełna implementacja nastąpi po zdefiniowaniu UserRepository.
 */
public class UniqueEmailValidatorImpl implements ConstraintValidator<UniqueEmailValidator, String> {

    // Używamy opóźnionego wstrzykiwania lub pomijamy autowired na tym etapie,
    // ponieważ UserRepository jeszcze nie istnieje.
    // Dodałem @Autowired dla czytelności, ale faktycznie zadziała po utworzeniu UserRepository.
    // @Autowired
    private UserRepository userRepository;

    // Settera do wstrzykiwania repozytorium (Spring użyje go automatycznie dla walidatorów)
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void initialize(UniqueEmailValidator constraintAnnotation) {
        // Tutaj można zainicjalizować walidator, jeśli potrzebne są jakieś parametry z adnotacji.
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        // Na razie zwracamy true, ponieważ UserRepository nie istnieje.
        // Prawdziwa logika będzie wyglądać tak:
        // return email != null && !userRepository.findByEmail(email).isPresent();
        return true;
    }
}