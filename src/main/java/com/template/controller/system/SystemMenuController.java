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
import com.template.dto.menu.MenuRequest;
import com.template.dto.menu.MenuResponse;
import com.template.service.menu.MenuService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/system/menus")
@PreAuthorize("hasRole('SYSTEM')")
@RequiredArgsConstructor
public class SystemMenuController {

    private final MenuService menuService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        PageResult<MenuResponse> result = menuService.findAll(keyword, page, size);
        model.addAttribute("menus", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        return "system/menu/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("menu", new MenuRequest());
        model.addAttribute("parentMenus", menuService.findAllMenus());
        return "system/menu/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("menu") MenuRequest request,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("parentMenus", menuService.findAllMenus());
            return "system/menu/form";
        }
        menuService.create(request);
        redirectAttributes.addFlashAttribute("successMessage", "Menu saved successfully");
        return "redirect:/system/menus";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        MenuResponse menu = menuService.getById(id);
        if (menu == null) {
            return "redirect:/system/menus";
        }
        model.addAttribute("menu", menu);
        model.addAttribute("parentMenus", menuService.findAllMenus());
        return "system/menu/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("menu") MenuRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("parentMenus", menuService.findAllMenus());
            return "system/menu/form";
        }
        request.setId(id);
        menuService.update(request);
        redirectAttributes.addFlashAttribute("successMessage", "Menu updated successfully");
        return "redirect:/system/menus";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        menuService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Menu deleted successfully");
        return "redirect:/system/menus";
    }
}