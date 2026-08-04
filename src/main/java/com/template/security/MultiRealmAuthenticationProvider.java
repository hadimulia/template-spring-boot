package com.template.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/**
 * Login provider for the multi-realm school system. Reads the {@code schoolCode}
 * from the authentication token's {@link SchoolCodeWebAuthenticationDetails},
 * delegates to {@link CustomUserDetailsService}, and verifies the password. This
 * replaces Spring's {@code DaoAuthenticationProvider} so the school code reaches
 * user loading (the default provider only passes the username).
 */
@Component
public class MultiRealmAuthenticationProvider implements AuthenticationProvider {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public MultiRealmAuthenticationProvider(CustomUserDetailsService userDetailsService,
                                            PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!supports(authentication.getClass())) {
            return null;
        }
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();
        String schoolCode = schoolCodeFrom(authentication);

        UserDetails user = userDetailsService.loadUserByUsername(username, schoolCode);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(
                user, user.getPassword(), user.getAuthorities());
        result.setDetails(authentication.getDetails());
        return result;
    }

    private String schoolCodeFrom(Authentication authentication) {
        if (authentication.getDetails() instanceof WebAuthenticationDetails details) {
            if (details instanceof SchoolCodeWebAuthenticationDetails sc) {
                String code = sc.getSchoolCode();
                if (code != null && !code.isBlank()) {
                    return code.trim();
                }
            }
        }
        throw new BadCredentialsException("School code is required");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}