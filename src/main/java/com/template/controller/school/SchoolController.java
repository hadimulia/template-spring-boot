package com.template.controller.school;

import com.template.dto.PageResult;
import com.template.dto.school.SchoolRequest;
import com.template.dto.school.SchoolResponse;
import com.template.service.school.SchoolService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_VIEW')")
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        PageResult<SchoolResponse> result = schoolService.findAll(keyword, page, size);
        model.addAttribute("schools", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        return "school/list";
    }

    @GetMapping("/api/list")
    @PreAuthorize("hasAuthority('SCHOOL_VIEW')")
    @ResponseBody
    public PageResult<SchoolResponse> listApi(@RequestParam(defaultValue = "") String keyword,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return schoolService.findAll(keyword, page, size);
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('SCHOOL_CREATE')")
    public String form(Model model) {
        model.addAttribute("school", new SchoolRequest());
        return "school/form";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_CREATE')")
    public String save(@Valid @ModelAttribute("school") SchoolRequest request,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "school/form";
        }
        try {
            schoolService.create(request);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/schools/new";
        }
        redirectAttributes.addFlashAttribute("successMessage", "School saved successfully");
        return "redirect:/schools";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('SCHOOL_EDIT')")
    public String edit(@PathVariable Long id, Model model) {
        SchoolResponse school = schoolService.getById(id);
        if (school == null) {
            return "redirect:/schools";
        }
        model.addAttribute("school", school);
        return "school/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_EDIT')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("school") SchoolRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "school/form";
        }
        schoolService.update(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "School updated successfully");
        return "redirect:/schools";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('SCHOOL_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        schoolService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "School deleted successfully");
        return "redirect:/schools";
    }
}
