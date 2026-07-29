package com.template.service.permission;

import com.template.dto.PageResult;
import com.template.dto.permission.PermissionRequest;
import com.template.dto.permission.PermissionResponse;
import com.template.entity.permission.Permission;
import com.template.mapper.permission.PermissionMapper;
import com.template.service.generic.GenericServiceImpl;
import com.template.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PermissionServiceImpl extends GenericServiceImpl<Permission, Long> implements PermissionService {

    private final PermissionMapper permissionMapper;

    public PermissionServiceImpl(PermissionMapper permissionMapper) {
        super(permissionMapper);
        this.permissionMapper = permissionMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<PermissionResponse> findAll(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<PermissionResponse> data = permissionMapper.findAll(keyword, offset, size);
        int total = permissionMapper.countAll(keyword);
        return PageResult.of(data, total, page, size);
    }

    public void create(PermissionRequest request) {
        Permission permission = new Permission();
        permission.setCode(request.getCode());
        permission.setDescription(request.getDescription());
        permission.setCreatedBy(SecurityUtils.getCurrentUsername());
        permission.setCreatedDate(LocalDateTime.now());
        permission.setDeleted(false);
        permission.setVersion(0);
        save(permission);
    }

    public void update(PermissionRequest request) {
        Permission permission = get(request.getId());
        if (permission != null) {
            permission.setCode(request.getCode());
            permission.setDescription(request.getDescription());
            permission.setUpdatedBy(SecurityUtils.getCurrentUsername());
            permission.setUpdatedDate(LocalDateTime.now());
            save(permission);
        }
    }

    public void delete(Long id) {
        Permission permission = get(id);
        if (permission != null) {
            permission.setDeleted(true);
            permission.setUpdatedBy(SecurityUtils.getCurrentUsername());
            permission.setUpdatedDate(LocalDateTime.now());
            save(permission);
        }
    }

    @Transactional(readOnly = true)
    public PermissionResponse getById(Long id) {
        Permission permission = get(id);
        if (permission == null || Boolean.TRUE.equals(permission.getDeleted())) return null;

        return PermissionResponse.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .description(permission.getDescription())
                .createdDate(permission.getCreatedDate())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> findAllForSelect() {
        List<Permission> permissions = permissionMapper.selectAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .collect(Collectors.toList());

        return permissions.stream()
                .map(p -> PermissionResponse.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .description(p.getDescription())
                        .build())
                .collect(Collectors.toList());
    }
}
