package com.template.service.permission;

import com.template.dto.PageResult;
import com.template.dto.PermissionRequest;
import com.template.dto.PermissionResponse;
import com.template.entity.Permission;
import com.template.service.generic.GenericService;

import java.util.List;

public interface PermissionService extends GenericService<Permission, Long> {

    PageResult<PermissionResponse> findAll(String keyword, int page, int size);

    void create(PermissionRequest request);

    void update(PermissionRequest request);

    void delete(Long id);

    PermissionResponse getById(Long id);

    List<PermissionResponse> findAllForSelect();
}
