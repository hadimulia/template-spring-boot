package com.template.controller.academic;

import com.template.dto.PageResult;
import com.template.dto.academic.StudentRequest;
import com.template.dto.academic.StudentResponse;
import com.template.mapper.academic.ClassMapper;
import com.template.service.academic.StudentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final ClassMapper classMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('STUDENT_VIEW')")
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        PageResult<StudentResponse> result = studentService.findAll(keyword, page, size);
        model.addAttribute("students", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        return "student/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('STUDENT_CREATE')")
    public String form(Model model) {
        model.addAttribute("student", new StudentRequest());
        addClasses(model);
        return "student/form";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STUDENT_CREATE')")
    public String save(@Valid @ModelAttribute("student") StudentRequest request,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addClasses(model);
            return "student/form";
        }
        studentService.create(request);
        redirectAttributes.addFlashAttribute("successMessage", "Student created");
        return "redirect:/students";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('STUDENT_EDIT')")
    public String edit(@PathVariable Long id, Model model) {
        StudentResponse student = studentService.getById(id);
        if (student == null) {
            return "redirect:/students";
        }
        StudentRequest form = new StudentRequest();
        form.setId(student.getId());
        form.setNis(student.getNis());
        form.setFullname(student.getFullname());
        form.setGender(student.getGender());
        form.setBirthDate(student.getBirthDate());
        form.setAddress(student.getAddress());
        form.setPhone(student.getPhone());
        form.setEmail(student.getEmail());
        form.setEnrollmentStatus(student.getEnrollmentStatus());
        form.setClassId(student.getClassId());
        model.addAttribute("student", form);
        addClasses(model);
        return "student/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('STUDENT_EDIT')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("student") StudentRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addClasses(model);
            return "student/form";
        }
        studentService.update(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "Student updated");
        return "redirect:/students";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('STUDENT_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        studentService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Student deleted");
        return "redirect:/students";
    }

    private void addClasses(Model model) {
        model.addAttribute("allClasses", classMapper.selectByExample(
                new tk.mybatis.mapper.entity.Condition(com.template.entity.academic.ClassEntity.class)));
    }
}