package com.template.service.user;

import com.template.dto.PageResult;
import com.template.dto.UserRequest;
import com.template.dto.UserResponse;
import com.template.dto.UserUpdateRequest;
import com.template.entity.User;
import com.template.service.generic.GenericService;

public interface UserService extends GenericService<User, Long> {

    PageResult<UserResponse> findAll(String keyword, int page, int size);

    void create(UserRequest request);

    void update(Long id, UserUpdateRequest request);

    void delete(Long id);

    UserResponse getById(Long id);
}
