package com.template.dto.user;

import java.util.List;

import com.template.validator.email.ValidateEmailExists;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {
    private Long id;

    @NotBlank(message = "{validation.username.required}")
    private String username;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 6, message = "{validation.password.minlength}")
    private String password;

    @NotBlank(message = "{validation.fullname.required}")
    private String fullname;

    @Email(message = "{validation.email.invalid}")
    @ValidateEmailExists
    private String email;

    private List<Long> roleIds;
}
