package com.template.controller.academic;

import com.template.dto.PageResult;
import com.template.dto.academic.TeacherRequest;
import com.template.dto.academic.TeacherResponse;
import com.template.service.academic.TeacherService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping
    @PreAuthorize("hasAuthority('TEACHER_VIEW')")
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        PageResult<TeacherResponse> result = teacherService.findAll(keyword, page, size);
        model.addAttribute("teachers", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        return "teacher/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('TEACHER_CREATE')")
    public String form(Model model) {
        model.addAttribute("teacher", new TeacherRequest());
        return "teacher/form";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TEACHER_CREATE')")
    public String save(@Valid @ModelAttribute("teacher") TeacherRequest request,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "teacher/form";
        }
        teacherService.create(request);
        redirectAttributes.addFlashAttribute("successMessage", "Teacher created");
        return "redirect:/teachers";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('TEACHER_EDIT')")
    public String edit(@PathVariable Long id, Model model) {
        TeacherResponse teacher = teacherService.getById(id);
        if (teacher == null) {
            return "redirect:/teachers";
        }
        TeacherRequest form = new TeacherRequest();
        form.setId(teacher.getId());
        form.setNip(teacher.getNip());
        form.setFullname(teacher.getFullname());
        form.setGender(teacher.getGender());
        form.setBirthDate(teacher.getBirthDate());
        form.setAddress(teacher.getAddress());
        form.setPhone(teacher.getPhone());
        form.setEmail(teacher.getEmail());
        form.setHireDate(teacher.getHireDate());
        model.addAttribute("teacher", form);
        return "teacher/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('TEACHER_EDIT')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("teacher") TeacherRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "teacher/form";
        }
        teacherService.update(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "Teacher updated");
        return "redirect:/teachers";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('TEACHER_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        teacherService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Teacher deleted");
        return "redirect:/teachers";
    }
}