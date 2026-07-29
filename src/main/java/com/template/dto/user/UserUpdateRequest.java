package com.template.dto.user;

import java.util.List;

import com.template.validator.user.ValidUserForm;

import lombok.Data;

@Data
@ValidUserForm
public class UserUpdateRequest {
	private Long id;
    private String username;
    private String password;
    private String fullname;
    private String email;

    private Boolean enabled;

    private List<Long> roleIds;
}
