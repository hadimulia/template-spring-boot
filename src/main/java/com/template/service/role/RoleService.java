package com.template.service.role;

import com.template.dto.PageResult;
import com.template.dto.RoleRequest;
import com.template.dto.RoleResponse;
import com.template.entity.Role;
import com.template.entity.RoleMenu;
import com.template.entity.RolePermission;
import com.template.mapper.*;
import com.template.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {

    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final PermissionMapper permissionMapper;
    private final MenuMapper menuMapper;

    @Transactional(readOnly = true)
    public PageResult<RoleResponse> findAll(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<RoleResponse> data = roleMapper.findAll(keyword, offset, size);
        int total = roleMapper.countAll(keyword);

        for (RoleResponse role : data) {
            List<Long> permissionIds = rolePermissionMapper.findPermissionIdsByRoleId(role.getId());
            role.setPermissionIds(permissionIds);
            List<Long> menuIds = roleMenuMapper.findMenuIdsByRoleId(role.getId());
            role.setMenuIds(menuIds);
        }

        return PageResult.of(data, total, page, size);
    }

    public void create(RoleRequest request) {
        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setCreatedBy(SecurityUtils.getCurrentUsername());
        role.setCreatedDate(LocalDateTime.now());
        role.setDeleted(false);
        role.setVersion(0);
        roleMapper.insert(role);

        if (request.getPermissionIds() != null) {
            for (Long permissionId : request.getPermissionIds()) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(permissionId);
                rp.setCreatedBy(SecurityUtils.getCurrentUsername());
                rp.setCreatedDate(LocalDateTime.now());
                rolePermissionMapper.insert(rp);
            }
        }

        if (request.getMenuIds() != null) {
            for (Long menuId : request.getMenuIds()) {
                RoleMenu rm = new RoleMenu();
                rm.setRoleId(role.getId());
                rm.setMenuId(menuId);
                rm.setCreatedBy(SecurityUtils.getCurrentUsername());
                rm.setCreatedDate(LocalDateTime.now());
                roleMenuMapper.insert(rm);
            }
        }
    }

    public void update(Long id, RoleRequest request) {
        Role role = roleMapper.selectByPrimaryKey(id);
        if (role != null) {
            role.setName(request.getName());
            role.setDescription(request.getDescription());
            role.setUpdatedBy(SecurityUtils.getCurrentUsername());
            role.setUpdatedDate(LocalDateTime.now());
            roleMapper.updateByPrimaryKey(role);

            // Update permissions
            if (request.getPermissionIds() != null) {
                List<Long> existingPermIds = rolePermissionMapper.findPermissionIdsByRoleId(id);
                for (Long permId : existingPermIds) {
                    RolePermission rp = new RolePermission();
                    rp.setRoleId(id);
                    rp.setPermissionId(permId);
                    rolePermissionMapper.delete(rp);
                }
                for (Long permissionId : request.getPermissionIds()) {
                    RolePermission rp = new RolePermission();
                    rp.setRoleId(id);
                    rp.setPermissionId(permissionId);
                    rp.setCreatedBy(SecurityUtils.getCurrentUsername());
                    rp.setCreatedDate(LocalDateTime.now());
                    rolePermissionMapper.insert(rp);
                }
            }

            // Update menus
            if (request.getMenuIds() != null) {
                List<Long> existingMenuIds = roleMenuMapper.findMenuIdsByRoleId(id);
                for (Long menuId : existingMenuIds) {
                    RoleMenu rm = new RoleMenu();
                    rm.setRoleId(id);
                    rm.setMenuId(menuId);
                    roleMenuMapper.delete(rm);
                }
                for (Long menuId : request.getMenuIds()) {
                    RoleMenu rm = new RoleMenu();
                    rm.setRoleId(id);
                    rm.setMenuId(menuId);
                    rm.setCreatedBy(SecurityUtils.getCurrentUsername());
                    rm.setCreatedDate(LocalDateTime.now());
                    roleMenuMapper.insert(rm);
                }
            }
        }
    }

    public void delete(Long id) {
        Role role = roleMapper.selectByPrimaryKey(id);
        if (role != null) {
            role.setDeleted(true);
            role.setUpdatedBy(SecurityUtils.getCurrentUsername());
            role.setUpdatedDate(LocalDateTime.now());
            roleMapper.updateByPrimaryKey(role);
        }
    }

    @Transactional(readOnly = true)
    public RoleResponse getById(Long id) {
        Role role = roleMapper.selectByPrimaryKey(id);
        if (role == null || Boolean.TRUE.equals(role.getDeleted())) return null;

        List<Long> permissionIds = rolePermissionMapper.findPermissionIdsByRoleId(id);
        List<Long> menuIds = roleMenuMapper.findMenuIdsByRoleId(id);

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissionIds(permissionIds)
                .menuIds(menuIds)
                .createdBy(role.getCreatedBy())
                .createdDate(role.getCreatedDate())
                .updatedBy(role.getUpdatedBy())
                .updatedDate(role.getUpdatedDate())
                .build();
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        List<Role> roles = roleMapper.selectAll().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
                .collect(Collectors.toList());

        return roles.stream().map(r -> RoleResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .description(r.getDescription())
                .build()).collect(Collectors.toList());
    }
}
