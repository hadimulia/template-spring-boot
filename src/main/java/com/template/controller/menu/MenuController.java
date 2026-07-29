package com.template.controller.menu;

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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.template.dto.menu.MenuRequest;
import com.template.dto.menu.MenuResponse;
import com.template.dto.PageResult;
import com.template.service.menu.MenuService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    @PreAuthorize("hasAuthority('MENU_VIEW')")
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        PageResult<MenuResponse> result = menuService.findAll(keyword, page, size);
        model.addAttribute("menus", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        return "menu/list";
    }

    @GetMapping("/api/list")
    @PreAuthorize("hasAuthority('MENU_VIEW')")
    @ResponseBody
    public PageResult<MenuResponse> listApi(@RequestParam(defaultValue = "") String keyword,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return menuService.findAll(keyword, page, size);
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('MENU_CREATE')")
    public String form(Model model) {
        model.addAttribute("menu", new MenuRequest());
        model.addAttribute("parentMenus", menuService.findAllMenus());
        return "menu/form";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MENU_CREATE')")
    public String save(@Valid @ModelAttribute("menu") MenuRequest request,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("parentMenus", menuService.findAllMenus());
            return "menu/form";
        }
        menuService.create(request);
        redirectAttributes.addFlashAttribute("successMessage", "Menu saved successfully");
        return "redirect:/menus";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('MENU_EDIT')")
    public String edit(@PathVariable Long id, Model model) {
        MenuResponse menu = menuService.getById(id);
        if (menu == null) {
            return "redirect:/menus";
        }
        model.addAttribute("menu", menu);
        model.addAttribute("parentMenus", menuService.findAllMenus());
        return "menu/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('MENU_EDIT')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("menu") MenuRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("parentMenus", menuService.findAllMenus());
            return "menu/form";
        }
        menuService.update(request);
        redirectAttributes.addFlashAttribute("successMessage", "Menu updated successfully");
        return "redirect:/menus";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('MENU_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        menuService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Menu deleted successfully");
        return "redirect:/menus";
    }
}
