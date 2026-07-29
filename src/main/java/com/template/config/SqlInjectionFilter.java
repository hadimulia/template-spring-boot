package com.template.config;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.template.util.SqlInjectionUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(1)
@Slf4j
public class SqlInjectionFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/") || path.startsWith("/js/")
            || path.startsWith("/webjars/") || path.startsWith("/images/")
            || path.startsWith("/actuator/") || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(new SqlRequestWrapper(request, request.getRequestURI()), response);
    }

    private static class SqlRequestWrapper extends HttpServletRequestWrapper {

        private final String requestUri;

        SqlRequestWrapper(HttpServletRequest request, String requestUri) {
            super(request);
            this.requestUri = requestUri;
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return validate(name, value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            for (int i = 0; i < values.length; i++) {
            	log.debug("Validating Sql Injection parameter: uri={} param={} value={}", requestUri, name, values[i]);
                values[i] = validate(name, values[i]);
            }
            return values;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return validate(name, value);
        }

        private String validate(String name, String value) {
            if (value != null && !value.isBlank()) {
                try {
                    SqlInjectionUtil.validate(value);
                } catch (IllegalArgumentException e) {
                    log.warn("SQL injection blocked: uri={} param={} value={}", requestUri, name, value);
                    throw e;
                }
            }
            return value;
        }
    }
}
