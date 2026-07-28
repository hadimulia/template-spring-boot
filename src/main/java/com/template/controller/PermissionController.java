package com.template.controller;

import com.template.dto.PageResult;
import com.template.dto.PermissionRequest;
import com.template.dto.PermissionResponse;
import com.template.service.permission.PermissionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@Controller
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSION_VIEW')")
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        PageResult<PermissionResponse> result = permissionService.findAll(keyword, page, size);
        model.addAttribute("permissions", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        return "permission/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('PERMISSION_CREATE')")
    public String form(Model model) {
        model.addAttribute("permission", new PermissionRequest());
        return "permission/form";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_CREATE')")
    public String save(@Valid @ModelAttribute("permission") PermissionRequest request,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "permission/form";
        }
        permissionService.create(request);
        redirectAttributes.addFlashAttribute("successMessage", "Permission saved successfully");
        return "redirect:/permissions";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('PERMISSION_EDIT')")
    public String edit(@PathVariable Long id, Model model) {
        PermissionResponse permission = permissionService.getById(id);
        if (permission == null) {
            return "redirect:/permissions";
        }
        model.addAttribute("permission", permission);
        return "permission/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_EDIT')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("permission") PermissionRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "permission/form";
        }
        request.setId(id);
        permissionService.update(request);
        redirectAttributes.addFlashAttribute("successMessage", "Permission updated successfully");
        return "redirect:/permissions";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('PERMISSION_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        permissionService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Permission deleted successfully");
        return "redirect:/permissions";
    }
}
