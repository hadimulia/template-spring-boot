package com.template.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.template.service.session.SessionStore;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final SessionStore sessionStore;

    public CustomAuthenticationSuccessHandler(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
        setDefaultTargetUrl("/dashboard");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String sessionId = request.getSession().getId();
        String username = authentication.getName();
        String ip = request.getRemoteAddr();
        String browser = request.getHeader("User-Agent");

        sessionStore.register(sessionId, username, ip, browser != null ? browser : "Unknown");

        getRedirectStrategy().sendRedirect(request, response, getDefaultTargetUrl());
    }
}
