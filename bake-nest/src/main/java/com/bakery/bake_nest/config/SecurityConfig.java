package com.bakery.bake_nest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Guests can place orders. Other components still require login.
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/orders/new", "/orders", "/orders/created",
                        "/css/orders.css", "/js/orders.js", "/error").permitAll()
                .anyRequest().authenticated());
        http.formLogin(login -> login.permitAll());
        // Keep CSRF protection enabled for form submissions.
        return http.build();
    }
}
