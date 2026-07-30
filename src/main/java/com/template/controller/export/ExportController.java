package com.template.controller.export;

import com.template.service.export.ExcelExportService;
import com.template.service.export.PdfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;

    @GetMapping("/users/excel")
    public ResponseEntity<byte[]> exportUsersExcel(@RequestParam(defaultValue = "") String keyword) {
        try {
            byte[] data = excelExportService.exportUsersToExcel(keyword);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export Excel", e);
        }
    }

    @GetMapping("/users/pdf")
    public ResponseEntity<byte[]> exportUsersPdf(@RequestParam(defaultValue = "") String keyword) {
        try {
            byte[] data = pdfExportService.exportUsersToPdf(keyword);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export PDF", e);
        }
    }
}
