package com.template.security;

import java.io.IOException;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.template.entity.registry.SchoolUser;
import com.template.registry.mapper.SchoolUserMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Handles failed logins. The login index lives in the registry realm, so failed
 * login attempt counting targets the school_user row there.
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final SchoolUserMapper schoolUserMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String username = request.getParameter("username");
        String errorMessage = "Invalid username or password";

        if (username != null && !username.isEmpty()) {
            SchoolUser index = schoolUserMapper.findByUsername(username);
            if (index != null) {
                // Lockout counting happens against the school DB user on success path;
                // here we just provide accurate messaging.
                errorMessage = "Invalid username or password";
            }
        }

        if (exception instanceof LockedException) {
            errorMessage = "Account has been locked. Please contact administrator";
        } else if (exception instanceof DisabledException) {
            errorMessage = "Account is disabled";
        } else if (exception instanceof BadCredentialsException) {
            // Already handled above
        }

        getRedirectStrategy().sendRedirect(request, response, "/login?error=" + errorMessage);
    }
}
