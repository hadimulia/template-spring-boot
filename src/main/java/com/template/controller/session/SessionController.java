package com.template.controller.session;

import com.template.dto.session.SessionInfo;
import com.template.service.session.SessionService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    @PreAuthorize("hasAuthority('SESSION_VIEW')")
    public String list(Model model, Authentication authentication) {
        List<SessionInfo> sessions = sessionService.getActiveSessions(authentication.getName());
        model.addAttribute("sessions", sessions);
        return "session/list";
    }

    @PostMapping("/force-logout")
    @PreAuthorize("hasAuthority('SESSION_KICK')")
    public String forceLogout(@RequestParam String sessionId,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        // Don't allow force-logout of your own session
        List<SessionInfo> sessions = sessionService.getActiveSessions(authentication.getName());
        for (SessionInfo s : sessions) {
            if (s.getSessionId().equals(sessionId) && s.isCurrentSession()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cannot force logout your own session");
                return "redirect:/sessions";
            }
        }

        sessionService.forceLogout(sessionId);
        redirectAttributes.addFlashAttribute("successMessage", "Session terminated successfully");
        return "redirect:/sessions";
    }
}
