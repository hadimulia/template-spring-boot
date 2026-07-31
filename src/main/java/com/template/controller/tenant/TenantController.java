package com.template.controller.tenant;

import com.template.dto.PageResult;
import com.template.dto.tenant.TenantRequest;
import com.template.dto.tenant.TenantResponse;
import com.template.service.tenant.TenantService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize("hasAuthority('TENANT_VIEW')")
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        PageResult<TenantResponse> result = tenantService.findAll(keyword, page, size);
        model.addAttribute("tenants", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        return "tenant/list";
    }

    @GetMapping("/api/list")
    @PreAuthorize("hasAuthority('TENANT_VIEW')")
    @ResponseBody
    public PageResult<TenantResponse> listApi(@RequestParam(defaultValue = "") String keyword,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return tenantService.findAll(keyword, page, size);
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('TENANT_CREATE')")
    public String form(Model model) {
        model.addAttribute("tenant", new TenantRequest());
        return "tenant/form";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TENANT_CREATE')")
    public String save(@Valid @ModelAttribute("tenant") TenantRequest request,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "tenant/form";
        }
        tenantService.create(request);
        redirectAttributes.addFlashAttribute("successMessage", "Tenant saved successfully");
        return "redirect:/tenants";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('TENANT_EDIT')")
    public String edit(@PathVariable Long id, Model model) {
        TenantResponse tenant = tenantService.getById(id);
        if (tenant == null) {
            return "redirect:/tenants";
        }
        model.addAttribute("tenant", tenant);
        return "tenant/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('TENANT_EDIT')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("tenant") TenantRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "tenant/form";
        }
        tenantService.update(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "Tenant updated successfully");
        return "redirect:/tenants";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('TENANT_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        tenantService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Tenant deleted successfully");
        return "redirect:/tenants";
    }
}
