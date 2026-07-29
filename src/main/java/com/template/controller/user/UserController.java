package com.template.controller.user;

import com.template.dto.PageResult;
import com.template.dto.role.RoleResponse;
import com.template.dto.user.UserRequest;
import com.template.dto.user.UserResponse;
import com.template.dto.user.UserUpdateRequest;
import com.template.service.role.RoleService;
import com.template.service.user.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        PageResult<UserResponse> result = userService.findAll(keyword, page, size);
        model.addAttribute("users", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        return "user/list";
    }

    @GetMapping("/api/list")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @ResponseBody
    public PageResult<UserResponse> listApi(@RequestParam(defaultValue = "") String keyword,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return userService.findAll(keyword, page, size);
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public String form(Model model) {
        model.addAttribute("user", new UserRequest());
        List<RoleResponse> roles = roleService.findAll();
        model.addAttribute("allRoles", roles);
        return "user/form";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public String save(@Valid @ModelAttribute("user") UserRequest request,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            List<RoleResponse> roles = roleService.findAll();
            model.addAttribute("allRoles", roles);
            return "user/form";
        }
        userService.create(request);
        redirectAttributes.addFlashAttribute("successMessage", "User berhasil disimpan");
        return "redirect:/users";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public String edit(@PathVariable Long id, Model model) {
        UserResponse user = userService.getById(id);
        if (user == null) {
            return "redirect:/users";
        }
        model.addAttribute("user", user);
        List<RoleResponse> roles = roleService.findAll();
        model.addAttribute("allRoles", roles);
        return "user/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("user") UserUpdateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            List<RoleResponse> roles = roleService.findAll();
            model.addAttribute("allRoles", roles);
            model.addAttribute("user", request);
            return "user/form";
        }
        userService.update(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "User berhasil diupdate");
        return "redirect:/users";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "User berhasil dihapus");
        return "redirect:/users";
    }
}
