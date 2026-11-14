package com.bakery.bake_nest;

import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.repository.UserRepository;
import com.bakery.bake_nest.security.CurrentCustomerResolver;
import com.bakery.bake_nest.security.DatabaseUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentCustomerResolverTest {
    private UserRepository repository;
    private CurrentCustomerResolver resolver;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        resolver = new CurrentCustomerResolver(repository);
    }

    @Test
    void reloadsActiveUserByAuthenticatedDatabaseId() {
        User reloaded = new User();
        reloaded.setUsername("renamed-test-customer");
        when(repository.findByUsernameIgnoreCase("test-customer")).thenReturn(Optional.of(reloaded));

        assertSame(reloaded, resolver.resolve(authenticated()));

        verify(repository).findByUsernameIgnoreCase("test-customer");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsMissingAuthentication() {
        assertRejectedWithoutLookup(null);
    }

    @Test
    void rejectsAnonymousAuthenticationEvenWithDatabasePrincipal() {
        Authentication anonymous = new AnonymousAuthenticationToken("test-key", principal(),
                List.of(new SimpleGrantedAuthority("ANONYMOUS_TEST")));
        assertRejectedWithoutLookup(anonymous);
    }

    @Test
    void rejectsUnauthenticatedDatabasePrincipal() {
        assertRejectedWithoutLookup(UsernamePasswordAuthenticationToken.unauthenticated(principal(), null));
    }

    @Test
    void rejectsAuthenticatedStringPrincipal() {
        assertRejectedWithoutLookup(UsernamePasswordAuthenticationToken.authenticated("test-customer", null, List.of()));
    }

    @Test
    void rejectsJpaUserAsPrincipal() {
        assertRejectedWithoutLookup(UsernamePasswordAuthenticationToken.authenticated(new User(), null, List.of()));
    }

    @Test
    void resolvesSharedSpringUserPrincipal() {
        var principal = new org.springframework.security.core.userdetails.User("test-customer", "", List.of());
        User user = new User();
        when(repository.findByUsernameIgnoreCase("test-customer")).thenReturn(Optional.of(user));
        assertSame(user, resolver.resolve(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of())));
    }

    @Test
    void rejectsDeletedUser() {
        when(repository.findByUsernameIgnoreCase("test-customer")).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> resolver.resolve(authenticated()));
        verify(repository).findByUsernameIgnoreCase("test-customer");
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"INACTIVE", "DISABLED", ""})
    void rejectsUserWhoseCurrentStatusIsNotActive(String status) {
        User user = new User();
        user.setStatus(status);
        when(repository.findByUsernameIgnoreCase("test-customer")).thenReturn(Optional.of(user));
        assertThrows(AccessDeniedException.class, () -> resolver.resolve(authenticated()));
        verify(repository).findByUsernameIgnoreCase("test-customer");
        verifyNoMoreInteractions(repository);
    }

    private DatabaseUserPrincipal principal() {
        // Resolution needs no password; it relies on the authenticated ID.
        return new DatabaseUserPrincipal(42L, "test-customer", "", true);
    }

    private Authentication authenticated() {
        return UsernamePasswordAuthenticationToken.authenticated(principal(), null, List.of());
    }

    private void assertRejectedWithoutLookup(Authentication authentication) {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> resolver.resolve(authentication));
        verifyNoInteractions(repository);
    }
}
