package com.milosz.podsiadly.common.util;

import com.milosz.podsiadly.common.exception.InvalidInputException;

public final class ValidationUtil {

    private ValidationUtil() {
        // Utility class
    }

    public static void validateNotNull(Object obj, String fieldName) {
        if (obj == null) {
            throw new InvalidInputException(fieldName + " cannot be null.");
        }
    }

    public static void validateNotBlank(String str, String fieldName) {
        if (str == null || str.trim().isEmpty()) {
            throw new InvalidInputException(fieldName + " cannot be null or empty.");
        }
    }

    public static void validatePositive(Number number, String fieldName) {
        if (number == null || number.doubleValue() <= 0) {
            throw new InvalidInputException(fieldName + " must be positive.");
        }
    }
}
