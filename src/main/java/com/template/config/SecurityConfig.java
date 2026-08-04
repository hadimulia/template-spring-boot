package com.template.config;

import com.template.security.CustomAuthenticationFailureHandler;
import com.template.security.CustomAuthenticationSuccessHandler;
import com.template.security.CustomUserDetailsService;
import com.template.security.SchoolCodeAuthenticationDetailsSource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAuthenticationFailureHandler failureHandler;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**",
                                "/webjars/**", "/error/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .authenticationDetailsSource(new SchoolCodeAuthenticationDetailsSource())
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .rememberMeServices(rememberMeServices())
                )
                .sessionManagement(session -> session
                        .maximumSessions(10)
                        .expiredUrl("/login?expired")
                        .sessionRegistry(sessionRegistry)
                );

        return http.build();
    }

    @Bean
    RememberMeServices rememberMeServices() {
        TokenBasedRememberMeServices rememberMe = new TokenBasedRememberMeServices(
                "templateAppSecretKey",
                userDetailsService
        );
        rememberMe.setParameter("remember-me");
        rememberMe.setTokenValiditySeconds(86400);
        return rememberMe;
    }
}
