package com.template.controller.audit;

import com.template.dto.PageResult;
import com.template.dto.audit.AuditLogResponse;
import com.template.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        PageResult<AuditLogResponse> result = auditService.findAll(keyword, page, size);
        model.addAttribute("logs", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        return "audit-log/list";
    }

    @GetMapping("/api/list")
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    @ResponseBody
    public PageResult<AuditLogResponse> listApi(@RequestParam(defaultValue = "") String keyword,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return auditService.findAll(keyword, page, size);
    }
}
