package com.template.controller.academic;

import com.template.dto.PageResult;
import com.template.dto.academic.ClassRequest;
import com.template.dto.academic.ClassResponse;
import com.template.entity.academic.Student;
import com.template.mapper.academic.StudentMapper;
import com.template.mapper.academic.TeacherMapper;
import com.template.service.academic.ClassService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import tk.mybatis.mapper.entity.Condition;

@Controller
@RequestMapping("/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;
    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('CLASS_VIEW')")
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        PageResult<ClassResponse> result = classService.findAll(keyword, page, size);
        model.addAttribute("classes", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        return "class/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('CLASS_CREATE')")
    public String form(Model model) {
        model.addAttribute("class", new ClassRequest());
        addFormData(model, null);
        return "class/form";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CLASS_CREATE')")
    public String save(@Valid @ModelAttribute("class") ClassRequest request,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormData(model, null);
            return "class/form";
        }
        classService.create(request);
        redirectAttributes.addFlashAttribute("successMessage", "Class created");
        return "redirect:/classes";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('CLASS_EDIT')")
    public String edit(@PathVariable Long id, Model model) {
        ClassResponse cls = classService.getById(id);
        if (cls == null) {
            return "redirect:/classes";
        }
        ClassRequest form = new ClassRequest();
        form.setId(cls.getId());
        form.setName(cls.getName());
        form.setGrade(cls.getGrade());
        form.setAcademicYear(cls.getAcademicYear());
        form.setHomeroomTeacherId(cls.getHomeroomTeacherId());
        form.setStudentIds(cls.getStudentIds());
        model.addAttribute("class", form);
        addFormData(model, cls.getStudentIds());
        return "class/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('CLASS_EDIT')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("class") ClassRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormData(model, null);
            return "class/form";
        }
        classService.update(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "Class updated");
        return "redirect:/classes";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('CLASS_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        classService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Class deleted");
        return "redirect:/classes";
    }

    private void addFormData(Model model, java.util.List<Long> selected) {
        Condition tCond = new Condition(com.template.entity.academic.Teacher.class);
        tCond.createCriteria().andEqualTo("deleted", false);
        model.addAttribute("allTeachers", teacherMapper.selectByExample(tCond));

        Condition sCond = new Condition(Student.class);
        sCond.createCriteria().andEqualTo("deleted", false);
        model.addAttribute("allStudents", studentMapper.selectByExample(sCond));
        model.addAttribute("selectedStudentIds", selected != null ? selected : java.util.List.of());
    }
}