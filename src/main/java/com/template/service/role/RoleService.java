package com.template.service.role;

import com.template.dto.PageResult;
import com.template.dto.RoleRequest;
import com.template.dto.RoleResponse;
import com.template.entity.Role;
import com.template.service.generic.GenericService;

import java.util.List;

public interface RoleService extends GenericService<Role, Long> {

    PageResult<RoleResponse> findAll(String keyword, int page, int size);

    void create(RoleRequest request);

    void update(Long id, RoleRequest request);

    void delete(Long id);

    RoleResponse getById(Long id);

    List<RoleResponse> findAll();
}
