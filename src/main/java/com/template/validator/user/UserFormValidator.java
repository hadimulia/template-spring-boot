package com.template.validator.user;

import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import com.template.dto.user.UserUpdateRequest;
import com.template.service.user.UserService;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserFormValidator implements ConstraintValidator<ValidUserForm, UserUpdateRequest>  {

    private final UserService userService;


    @Override
    public boolean isValid(UserUpdateRequest form,
                           ConstraintValidatorContext context) {

        boolean valid = true;

        context.disableDefaultConstraintViolation();

        // password required for create
        if (
            (!ObjectUtils.isEmpty(form.getPassword()) && form.getPassword().length() < 6) ) {

            context.buildConstraintViolationWithTemplate("Password must be at least 6 characters")
                    .addPropertyNode("password")
                    .addConstraintViolation();

            valid = false;
        }

        // email exists
        if (!ObjectUtils.isEmpty(form.getEmail()) && !ObjectUtils.isEmpty(userService.getByEmail(form.getEmail()))) {

            context.buildConstraintViolationWithTemplate("Email already exists")
                    .addPropertyNode("email")
                    .addConstraintViolation();

            valid = false;
        }
        
		if (ObjectUtils.isEmpty(form.getFullname())) {

			context.buildConstraintViolationWithTemplate("Fullname is required")
					.addPropertyNode("fullname")
					.addConstraintViolation();

			valid = false;
		}
        
        

        return valid;
    }
}
