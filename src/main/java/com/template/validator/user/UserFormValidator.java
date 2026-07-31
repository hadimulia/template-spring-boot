package com.template.validator.user;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import com.template.dto.user.UserUpdateRequest;
import com.template.entity.user.User;
import com.template.service.user.UserService;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserFormValidator implements ConstraintValidator<ValidUserForm, UserUpdateRequest>  {

    private final UserService userService;
    private final MessageSource messageSource;

    @Override
    public boolean isValid(UserUpdateRequest form,
                           ConstraintValidatorContext context) {

        boolean valid = true;

        context.disableDefaultConstraintViolation();
        User user = userService.get(form.getId());

        if (!ObjectUtils.isEmpty(form.getPassword()) && form.getPassword().length() < 6) {
            context.buildConstraintViolationWithTemplate(resolve("validation.password.minlength"))
                    .addPropertyNode("password")
                    .addConstraintViolation();
            valid = false;
        }

        User existingUser = userService.getByEmail(form.getEmail());
        if (!ObjectUtils.isEmpty(form.getEmail()) && !ObjectUtils.isEmpty(existingUser) && !existingUser.getId().equals(form.getId())) {
            context.buildConstraintViolationWithTemplate(resolve("validation.email.exists"))
                    .addPropertyNode("email")
                    .addConstraintViolation();
            valid = false;
        }

        if (ObjectUtils.isEmpty(form.getFullname())) {
            context.buildConstraintViolationWithTemplate(resolve("validation.fullname.required"))
                    .addPropertyNode("fullname")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }

    private String resolve(String code) {
        return messageSource.getMessage(code, null, code, LocaleContextHolder.getLocale());
    }
}
