package com.template.controller.file;

import com.template.dto.file.FileUploadResponse;
import com.template.entity.file.FileUpload;
import com.template.mapper.file.FileUploadMapper;
import com.template.service.file.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;
import java.util.List;

@Controller
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;
    private final FileUploadMapper fileUploadMapper;

    @GetMapping
    public String list(Model model) {
        List<FileUploadResponse> fileList = fileUploadService.findAll();
        model.addAttribute("fileList", fileList);
        return "file/list";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                          @RequestParam("entityType") String entityType,
                          @RequestParam(value = "entityId", required = false) Long entityId,
                          RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a file");
            return "redirect:/files";
        }
        fileUploadService.upload(file, entityType, entityId);
        redirectAttributes.addFlashAttribute("successMessage", "File uploaded: " + file.getOriginalFilename());
        return "redirect:/files";
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @ResponseBody
    public List<FileUploadResponse> getFiles(@PathVariable String entityType, @PathVariable Long entityId) {
        return fileUploadService.findByEntity(entityType, entityId);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        FileUpload entity = fileUploadMapper.selectByPrimaryKey(id);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }
        Path filePath = Path.of("uploads").resolve(entity.getStoredName());
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + entity.getOriginalName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        fileUploadService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "File deleted");
        return "redirect:/files";
    }
}
