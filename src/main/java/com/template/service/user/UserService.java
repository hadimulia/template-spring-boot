package com.template.service.user;

import com.template.dto.PageResult;
import com.template.dto.user.UserRequest;
import com.template.dto.user.UserResponse;
import com.template.dto.user.UserUpdateRequest;
import com.template.entity.user.User;
import com.template.service.generic.GenericService;

public interface UserService extends GenericService<User, Long> {

    PageResult<UserResponse> findAll(String keyword, int page, int size);

    void create(UserRequest request);

    void update(Long id, UserUpdateRequest request);

    void delete(Long id);

    UserResponse getById(Long id);
}
