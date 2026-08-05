package com.template.controller.system;

import java.util.List;

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
import com.template.dto.role.RoleResponse;
import com.template.dto.school.SchoolResponse;
import com.template.dto.user.UserRequest;
import com.template.dto.user.UserResponse;
import com.template.dto.user.UserUpdateRequest;
import com.template.registry.mapper.SchoolMapper;
import com.template.service.role.RoleService;
import com.template.service.system.SystemUserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/system/users")
@PreAuthorize("hasRole('SYSTEM')")
public class SystemUserController {

    private final SystemUserService systemUserService;
    private final SchoolMapper schoolMapper;
    private final RoleService roleService;

    public SystemUserController(SystemUserService systemUserService,
                                SchoolMapper schoolMapper,
                                RoleService roleService) {
        this.systemUserService = systemUserService;
        this.schoolMapper = schoolMapper;
        this.roleService = roleService;
    }

    private void addSchoolPicker(Model model, Long selectedSchoolId) {
        List<SchoolResponse> schools = schoolMapper.findAll(null, 0, 1000).stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .toList();
        model.addAttribute("schools", schools);
        model.addAttribute("selectedSchoolId", selectedSchoolId);
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long schoolId,
                       @RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        if (schoolId == null) {
            addSchoolPicker(model, null);
            model.addAttribute("users", List.of());
            model.addAttribute("pagination", null);
            model.addAttribute("keyword", keyword);
            return "user/list";
        }
        PageResult<UserResponse> result = systemUserService.listBySchool(schoolId, keyword, page, size);
        model.addAttribute("users", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        addSchoolPicker(model, schoolId);
        return "user/list";
    }

    @GetMapping("/new")
    public String form(@RequestParam Long schoolId, Model model) {
        model.addAttribute("user", new UserRequest());
        model.addAttribute("allRoles", roleService.findAll());
        addSchoolPicker(model, schoolId);
        return "user/form";
    }

    @PostMapping
    public String save(@RequestParam Long schoolId,
                       @Valid @ModelAttribute("user") UserRequest request,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allRoles", roleService.findAll());
            addSchoolPicker(model, schoolId);
            return "user/form";
        }
        systemUserService.create(schoolId, request);
        redirectAttributes.addFlashAttribute("successMessage", "User created in school");
        return "redirect:/system/users?schoolId=" + schoolId;
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @RequestParam Long schoolId, Model model) {
        PageResult<UserResponse> all = systemUserService.listBySchool(schoolId, "", 1, 1000);
        UserResponse found = all.getData().stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
        if (found == null) {
            return "redirect:/system/users?schoolId=" + schoolId;
        }
        UserUpdateRequest form = new UserUpdateRequest();
        form.setId(found.getId());
        form.setUsername(found.getUsername());
        form.setFullname(found.getFullname());
        form.setEmail(found.getEmail());
        form.setEnabled(found.getEnabled());
        model.addAttribute("allRoles", roleService.findAll());
        model.addAttribute("user", form);
        addSchoolPicker(model, schoolId);
        return "user/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam Long schoolId,
                         @Valid @ModelAttribute("user") UserUpdateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allRoles", roleService.findAll());
            addSchoolPicker(model, schoolId);
            return "user/form";
        }
        systemUserService.update(schoolId, id, request);
        redirectAttributes.addFlashAttribute("successMessage", "User updated in school");
        return "redirect:/system/users?schoolId=" + schoolId;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam Long schoolId,
                         RedirectAttributes redirectAttributes) {
        systemUserService.delete(schoolId, id);
        redirectAttributes.addFlashAttribute("successMessage", "User deleted from school");
        return "redirect:/system/users?schoolId=" + schoolId;
    }
}