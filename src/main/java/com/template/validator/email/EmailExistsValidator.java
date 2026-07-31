package com.template.validator.email;

import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import com.template.entity.user.User;
import com.template.service.user.UserService;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailExistsValidator implements ConstraintValidator<ValidateEmailExists, String> {

    private final UserService userService;

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (ObjectUtils.isEmpty(email)) {
            return true;
        }

        User existing = userService.getByEmail(email);
        return existing == null;
    }
}
