package com.template.security;

import java.io.IOException;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.template.entity.registry.SchoolUser;
import com.template.entity.school.School;
import com.template.registry.mapper.SchoolMapper;
import com.template.registry.mapper.SchoolUserMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Handles failed logins, producing distinct messages for unknown school code,
 * user not found in that school, and bad credentials.
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final String SYSTEM_CODE = "system";

    private final SchoolUserMapper schoolUserMapper;
    private final SchoolMapper schoolMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String schoolCode = request.getParameter("schoolCode");
        String username = request.getParameter("username");
        String errorMessage = "Invalid username or password";

        if (exception instanceof UsernameNotFoundException) {
            if (schoolCode != null && !schoolCode.isBlank()
                    && !SYSTEM_CODE.equalsIgnoreCase(schoolCode.trim())) {
                School school = schoolMapper.findByCode(schoolCode.trim());
                if (school == null || Boolean.TRUE.equals(school.getDeleted())
                        || !"ACTIVE".equals(school.getStatus())) {
                    errorMessage = "School not found or inactive: " + schoolCode;
                } else if (username != null && !username.isEmpty()) {
                    SchoolUser index = schoolUserMapper.findBySchoolAndUsername(
                            school.getId(), username);
                    errorMessage = (index == null)
                            ? "User not found in this school"
                            : "Invalid username or password";
                }
            } else {
                errorMessage = "User not found in system";
            }
        }

        if (exception instanceof LockedException) {
            errorMessage = "Account has been locked. Please contact administrator";
        } else if (exception instanceof DisabledException) {
            errorMessage = "Account is disabled";
        } else if (exception instanceof BadCredentialsException) {
            // keep "Invalid username or password"
        }

        getRedirectStrategy().sendRedirect(request, response, "/login?error=" + errorMessage);
    }
}