package com.template.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/** Builds {@link SchoolCodeWebAuthenticationDetails} for each login attempt. */
public class SchoolCodeAuthenticationDetailsSource
        implements AuthenticationDetailsSource<HttpServletRequest, WebAuthenticationDetails> {

    @Override
    public WebAuthenticationDetails buildDetails(HttpServletRequest context) {
        return new SchoolCodeWebAuthenticationDetails(context);
    }
}