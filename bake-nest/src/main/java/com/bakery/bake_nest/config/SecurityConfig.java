package com.bakery.bake_nest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Define security settings managed by Spring
@Configuration
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public org.springframework.security.web.SecurityFilterChain securityFilterChain(
            org.springframework.security.config.annotation.web.builders.HttpSecurity http,
            com.bakery.bake_nest.service.AccountUserDetailsService accounts)
            throws Exception {
        http.authorizeHttpRequests(rules ->
                rules.requestMatchers("/api/csrf", "/error", "/login")
                        .permitAll()
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST, "/api/users/register")
                        .permitAll()
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/products/**",
                                "/api/categories/**",
                                "/api/reviews",
                                "/api/reviews/product/**")
                        .permitAll()
                        .requestMatchers("/api/admins/**", "/api/roles/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated());
        // Keep session login, redirects, POST logout and CSRF protection.
        http.formLogin(login ->
                login.successHandler((request, response, authentication) -> {
                    // Remember which password was used for this session. Password changes revoke
                    // old sessions.
                    var account = accounts.loadUserByUsername(authentication.getName());
                    request.getSession()
                            .setAttribute("ACCOUNT_PASSWORD_HASH", account.getPassword());
                    new org.springframework.security.web.authentication
                                    .SavedRequestAwareAuthenticationSuccessHandler()
                            .onAuthenticationSuccess(request, response, authentication);
                }));
        http.logout(org.springframework.security.config.Customizer.withDefaults());
        http.exceptionHandling(errors ->
                errors.defaultAuthenticationEntryPointFor(
                        (request, response, exception) -> response.sendError(401, "Please log in."),
                        request -> request.getRequestURI().startsWith("/api/")));
        http.addFilterAfter(
                new com.bakery.bake_nest.shared.security.ActiveAccountFilter(accounts),
                org.springframework.security.web.authentication.AnonymousAuthenticationFilter
                        .class);
        return http.build();
    }

    // Register the password encoder for Spring Security
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use BCrypt to hash passwords and check them during login
        return new BCryptPasswordEncoder();
    }
}
