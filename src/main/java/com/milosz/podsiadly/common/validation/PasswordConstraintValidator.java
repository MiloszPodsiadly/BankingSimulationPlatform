package com.milosz.podsiadly.common.validation;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    // Regex dla hasła:
    // Minimum 8 znaków
    // Co najmniej jedna duża litera (?=.*[A-Z])
    // Co najmniej jedna mała litera (?=.*[a-z])
    // Co najmniej jedna cyfra (?=.*\\d)
    // Co najmniej jeden znak specjalny (?=.*[!@#$%^&*()_+\-=\\[\\]{};':"|,.<>/?])
    private static final String PASSWORD_PATTERN =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"|,.<>/?]).{8,}$";

    private Pattern pattern;

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        pattern = Pattern.compile(PASSWORD_PATTERN);
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }
}