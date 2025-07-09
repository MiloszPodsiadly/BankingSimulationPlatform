package com.milosz.podsiadly.common.util;


import java.util.regex.Pattern;

public class ValidationUtil {

    // Example: Basic email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$"
    );

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    // Example: Basic strong password check (if not using @ValidPassword annotation)
    // This is more of a fallback/alternative. The @ValidPassword is preferred for DTOs.
    public static boolean isStrongPassword(String password) {
        // At least 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&+=])(?=\\S+$).{8,}$";
        return password != null && Pattern.compile(regex).matcher(password).matches();
    }

    // Add other general validation methods here as needed
}