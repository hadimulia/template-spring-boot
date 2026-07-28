package com.template.controller;

import com.template.dto.PageResult;
import com.template.dto.PermissionResponse;
import com.template.entity.Permission;
import com.template.mapper.PermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionMapper permissionMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        int offset = (page - 1) * size;
        java.util.List<PermissionResponse> data = permissionMapper.findAll(keyword, offset, size);
        int total = permissionMapper.countAll(keyword);

        model.addAttribute("permissions", data);
        model.addAttribute("pagination", PageResult.Pagination.of(total, page, size));
        model.addAttribute("keyword", keyword);
        return "permission/list";
    }
}
