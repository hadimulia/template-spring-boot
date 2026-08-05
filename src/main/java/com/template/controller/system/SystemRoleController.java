package com.template.controller.system;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.template.dto.PageResult;
import com.template.dto.role.RoleRequest;
import com.template.dto.role.RoleResponse;
import com.template.service.menu.MenuService;
import com.template.service.permission.PermissionService;
import com.template.service.role.RoleService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/system/roles")
@PreAuthorize("hasRole('SYSTEM')")
public class SystemRoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final MenuService menuService;

    public SystemRoleController(RoleService roleService,
                                PermissionService permissionService,
                                MenuService menuService) {
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.menuService = menuService;
    }

    private void addFormModel(Model model) {
        model.addAttribute("allPermissions", permissionService.findAllForSelect());
        model.addAttribute("allMenus", menuService.findAllMenus());
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        PageResult<RoleResponse> result = roleService.findAll(keyword, page, size);
        model.addAttribute("roles", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        return "system/role/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("role", new RoleRequest());
        addFormModel(model);
        return "system/role/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("role") RoleRequest request,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormModel(model);
            return "system/role/form";
        }
        roleService.create(request);
        redirectAttributes.addFlashAttribute("successMessage", "Role created");
        return "redirect:/system/roles";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        RoleResponse role = roleService.getById(id);
        if (role == null) {
            return "redirect:/system/roles";
        }
        model.addAttribute("role", role);
        addFormModel(model);
        return "system/role/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("role") RoleRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormModel(model);
            return "system/role/form";
        }
        roleService.update(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "Role updated");
        return "redirect:/system/roles";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        roleService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Role deleted");
        return "redirect:/system/roles";
    }
}