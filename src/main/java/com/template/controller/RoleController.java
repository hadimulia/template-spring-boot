package com.template.controller;

import com.template.dto.*;
import com.template.service.menu.MenuServiceImpl;
import com.template.service.permission.PermissionService;
import com.template.service.role.RoleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final MenuServiceImpl menuService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        PageResult<RoleResponse> result = roleService.findAll(keyword, page, size);
        model.addAttribute("roles", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        return "role/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public String form(Model model) {
        model.addAttribute("role", new RoleRequest());
        model.addAttribute("allPermissions", permissionService.findAllForSelect());
        model.addAttribute("allMenus", menuService.findAllMenus());
        return "role/form";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public String save(@Valid @ModelAttribute("role") RoleRequest request,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allPermissions", permissionService.findAllForSelect());
            model.addAttribute("allMenus", menuService.findAllMenus());
            return "role/form";
        }
        roleService.create(request);
        redirectAttributes.addFlashAttribute("successMessage", "Role saved successfully");
        return "redirect:/roles";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('ROLE_EDIT')")
    public String edit(@PathVariable Long id, Model model) {
        RoleResponse role = roleService.getById(id);
        if (role == null) {
            return "redirect:/roles";
        }
        model.addAttribute("role", role);
        model.addAttribute("allPermissions", permissionService.findAllForSelect());
        model.addAttribute("allMenus", menuService.findAllMenus());
        return "role/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_EDIT')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("role") RoleRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allPermissions", permissionService.findAllForSelect());
            model.addAttribute("allMenus", menuService.findAllMenus());
            return "role/form";
        }
        roleService.update(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "Role updated successfully");
        return "redirect:/roles";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        roleService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Role deleted successfully");
        return "redirect:/roles";
    }
}
