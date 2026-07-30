package com.template.controller.export;

import com.template.service.export.ExcelImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/import")
@RequiredArgsConstructor
public class ImportController {

    private final ExcelImportService excelImportService;

    @GetMapping("/users")
    public String importForm() {
        return "import/user";
    }

    @PostMapping("/users")
    public String importUsers(@RequestParam("file") MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a file");
            return "redirect:/import/users";
        }
        try {
            List<String> results = excelImportService.importUsers(file);
            String lastMsg = results.get(results.size() - 1);
            if (lastMsg.startsWith("Successfully") || lastMsg.startsWith("Imported")) {
                redirectAttributes.addFlashAttribute("successMessage", lastMsg);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", lastMsg);
            }
            if (results.size() > 1) {
                redirectAttributes.addFlashAttribute("importDetails", results);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Import failed: " + e.getMessage());
        }
        return "redirect:/import/users";
    }
}
