package com.template.service.permission;

import com.template.dto.PermissionResponse;
import com.template.entity.Permission;
import com.template.mapper.PermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PermissionService {

    private final PermissionMapper permissionMapper;

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
