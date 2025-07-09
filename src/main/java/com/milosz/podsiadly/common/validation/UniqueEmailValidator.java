package com.milosz.podsiadly.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniqueEmailValidatorImpl.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueEmailValidator {
    String message() default "Email already exists.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
