package com.template.service.system;

import com.template.dto.PageResult;
import com.template.dto.user.UserRequest;
import com.template.dto.user.UserResponse;
import com.template.dto.user.UserUpdateRequest;

public interface SystemUserService {
    PageResult<UserResponse> listBySchool(Long schoolId, String keyword, int page, int size);
    void create(Long schoolId, UserRequest request);
    void update(Long schoolId, Long userId, UserUpdateRequest request);
    void delete(Long schoolId, Long userId);
}