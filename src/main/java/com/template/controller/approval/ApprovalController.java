package com.template.controller.approval;

import com.template.dto.PageResult;
import com.template.dto.approval.ApprovalRequestResponse;
import com.template.dto.approval.ReviewRequest;
import com.template.service.approval.ApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping
    @PreAuthorize("hasAuthority('APPROVAL_VIEW')")
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        PageResult<ApprovalRequestResponse> result = approvalService.findAll(keyword, page, size);
        model.addAttribute("requests", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        return "approval/list";
    }

    @GetMapping("/api/list")
    @PreAuthorize("hasAuthority('APPROVAL_VIEW')")
    @ResponseBody
    public PageResult<ApprovalRequestResponse> listApi(@RequestParam(defaultValue = "") String keyword,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return approvalService.findAll(keyword, page, size);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('APPROVAL_REVIEW')")
    public String pending(Model model) {
        model.addAttribute("requests", approvalService.findPending());
        return "approval/pending";
    }

    @PostMapping("/review")
    @PreAuthorize("hasAuthority('APPROVAL_REVIEW')")
    public String review(@Valid ReviewRequest request, RedirectAttributes redirectAttributes) {
        if ("APPROVED".equals(request.getAction())) {
            approvalService.approve(request.getId(), request.getReviewNotes());
            redirectAttributes.addFlashAttribute("successMessage", "Request approved");
        } else if ("REJECTED".equals(request.getAction())) {
            approvalService.reject(request.getId(), request.getReviewNotes());
            redirectAttributes.addFlashAttribute("successMessage", "Request rejected");
        }
        return "redirect:/approvals";
    }
}
