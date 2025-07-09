package com.milosz.podsiadly.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = UniqueEmailValidatorImpl.class) // Walidator będzie zaimplementowany w UniqueEmailValidatorImpl
@Target({ FIELD, ANNOTATION_TYPE })
@Retention(RUNTIME)
public @interface UniqueEmailValidator {
    String message() default "Email address is already in use.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}