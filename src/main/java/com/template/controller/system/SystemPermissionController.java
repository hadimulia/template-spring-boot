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
import com.template.dto.permission.PermissionRequest;
import com.template.dto.permission.PermissionResponse;
import com.template.service.permission.PermissionService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/system/permissions")
@PreAuthorize("hasRole('SYSTEM')")
public class SystemPermissionController {

    private final PermissionService permissionService;

    public SystemPermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        PageResult<PermissionResponse> result = permissionService.findAll(keyword, page, size);
        model.addAttribute("permissions", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        return "system/permission/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("permission", new PermissionRequest());
        return "system/permission/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("permission") PermissionRequest request,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "system/permission/form";
        }
        permissionService.create(request);
        redirectAttributes.addFlashAttribute("successMessage", "Permission created");
        return "redirect:/system/permissions";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        PermissionResponse permission = permissionService.getById(id);
        if (permission == null) {
            return "redirect:/system/permissions";
        }
        PermissionRequest form = new PermissionRequest();
        form.setId(permission.getId());
        form.setCode(permission.getCode());
        form.setDescription(permission.getDescription());
        model.addAttribute("permission", form);
        return "system/permission/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("permission") PermissionRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "system/permission/form";
        }
        request.setId(id);
        permissionService.update(request);
        redirectAttributes.addFlashAttribute("successMessage", "Permission updated");
        return "redirect:/system/permissions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        permissionService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Permission deleted");
        return "redirect:/system/permissions";
    }
}