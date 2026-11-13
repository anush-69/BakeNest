package com.bakery.bake_nest.config;

import com.bakery.bake_nest.security.DatabaseUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import jakarta.servlet.DispatcherType;
import java.util.regex.Pattern;

@Configuration
@Profile("booking-local")
public class BookingLocalSecurityConfig {
    @Bean
    public PasswordEncoder bookingPasswordEncoder() {
        return new StrictBcryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider bookingAuthenticationProvider(
            DatabaseUserDetailsService detailsService, PasswordEncoder bookingPasswordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(detailsService);
        provider.setPasswordEncoder(bookingPasswordEncoder);
        // Keep unknown usernames indistinguishable from incorrect passwords.
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    public SecurityFilterChain bookingSecurityFilterChain(HttpSecurity http,
            DaoAuthenticationProvider bookingAuthenticationProvider) throws Exception {
        http.authenticationProvider(bookingAuthenticationProvider)
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/login", "/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults())
                .logout(Customizer.withDefaults());
        // CSRF and session protection retain Spring Security defaults.
        return http.build();
    }

    private static final class StrictBcryptPasswordEncoder implements PasswordEncoder {
        private static final Pattern HASH = Pattern.compile(
                "\\A\\$2[aby]\\$(0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}\\z");
        private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

        @Override
        public String encode(CharSequence rawPassword) {
            return bcrypt.encode(rawPassword);
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            if (rawPassword == null || encodedPassword == null) {
                return false;
            }
            String hash = encodedPassword.startsWith("{bcrypt}")
                    ? encodedPassword.substring("{bcrypt}".length()) : encodedPassword;
            if (!HASH.matcher(hash).matches()) {
                return false;
            }
            try {
                return bcrypt.matches(rawPassword, hash);
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
    }
}
