package com.template.service.permission;

import com.template.dto.PermissionResponse;
import com.template.entity.Permission;
import com.template.service.generic.GenericService;

import java.util.List;

public interface PermissionService extends GenericService<Permission, Long> {

    List<PermissionResponse> findAllForSelect();
}
