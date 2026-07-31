package com.template.security;

import java.io.IOException;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.template.entity.user.User;
import com.template.mapper.user.UserMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final UserMapper userMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String username = request.getParameter("username");
        String errorMessage = "Invalid username or password";

        if (username != null && !username.isEmpty()) {
            User user = userMapper.findByUsername(username, null);
            if (user != null) {
                // Increment login attempts
                int attempts = user.getLoginAttempts() != null ? user.getLoginAttempts() + 1 : 1;
                user.setLoginAttempts(attempts);

                if (attempts >= 5) {
                    user.setAccountLocked(true);
                    errorMessage = "Account has been locked due to too many failed attempts";
                } else {
                    int remaining = 5 - attempts;
                    errorMessage = "Invalid username or password. " + remaining + " attempt(s) remaining";
                }

                userMapper.updateByPrimaryKey(user);
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
