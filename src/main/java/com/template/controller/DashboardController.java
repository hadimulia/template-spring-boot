package com.template.controller;

import com.template.mapper.menu.MenuMapper;
import com.template.mapper.permission.PermissionMapper;
import com.template.mapper.role.RoleMapper;
import com.template.mapper.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final PermissionMapper permissionMapper;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userMapper.countAllUsers());
        model.addAttribute("totalRoles", roleMapper.selectCount(null));
        model.addAttribute("totalMenus", menuMapper.selectCount(null));
        model.addAttribute("totalPermissions", permissionMapper.selectCount(null));
        return "dashboard";
    }
}
