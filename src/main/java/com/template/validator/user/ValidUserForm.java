package com.template.validator.user;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UserFormValidator.class)
public @interface ValidUserForm {
    String message() default "Invalid user data";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}