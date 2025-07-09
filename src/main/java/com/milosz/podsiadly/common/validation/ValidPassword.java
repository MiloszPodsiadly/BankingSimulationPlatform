package com.milosz.podsiadly.common.validation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = { PasswordConstraintValidator.class })
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "Password must be at least 8 characters long, contain uppercase, lowercase, a digit, and a special character.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
