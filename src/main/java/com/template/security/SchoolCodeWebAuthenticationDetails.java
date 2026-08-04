package com.template.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/** Carries the login form's {@code schoolCode} param to the auth flow. */
public class SchoolCodeWebAuthenticationDetails extends WebAuthenticationDetails {

    private final String schoolCode;

    public SchoolCodeWebAuthenticationDetails(HttpServletRequest request) {
        super(request);
        this.schoolCode = request.getParameter("schoolCode");
    }

    public String getSchoolCode() {
        return schoolCode;
    }
}