package com.bakery.bake_nest.shared.security;

import com.bakery.bake_nest.service.AccountUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Inheritance lets this filter reuse Spring's once-per-request behaviour.
public class ActiveAccountFilter extends OncePerRequestFilter {
    private final AccountUserDetailsService accounts;

    public ActiveAccountFilter(AccountUserDetailsService accounts) {
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            try {
                var current = accounts.loadUserByUsername(authentication.getName());
                if (!current.isEnabled()) {
                    throw new UsernameNotFoundException("Account disabled");
                }
                var session = request.getSession(false);
                if (session != null
                        && session.getAttribute("ACCOUNT_PASSWORD_HASH") != null
                        && !current.getPassword()
                                .equals(session.getAttribute("ACCOUNT_PASSWORD_HASH"))) {
                    throw new UsernameNotFoundException("Password changed");
                }
                // Refresh roles so an old session cannot keep removed access.
                var refreshed =
                        UsernamePasswordAuthenticationToken.authenticated(
                                current, null, current.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(refreshed);
            } catch (UsernameNotFoundException exception) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) request.getSession(false).invalidate();
                response.sendError(401, "Account is no longer active.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
