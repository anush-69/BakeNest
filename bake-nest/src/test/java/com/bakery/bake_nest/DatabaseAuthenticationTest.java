package com.bakery.bake_nest;

import com.bakery.bake_nest.config.BookingLocalSecurityConfig;
import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.repository.UserRepository;
import com.bakery.bake_nest.security.DatabaseUserDetailsService;
import com.bakery.bake_nest.security.DatabaseUserPrincipal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseAuthenticationTest {
    private static String password;
    private static String hash;
    private UserRepository repository;
    private DatabaseUserDetailsService detailsService;
    private PasswordEncoder encoder;
    private ProviderManager manager;

    @BeforeAll
    static void createSyntheticCredentials() {
        // These credentials exist only in memory and are never printed.
        password = UUID.randomUUID().toString();
        hash = new BCryptPasswordEncoder().encode(password);
    }

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        detailsService = new DatabaseUserDetailsService(repository);
        BookingLocalSecurityConfig config = new BookingLocalSecurityConfig();
        encoder = config.bookingPasswordEncoder();
        manager = new ProviderManager(config.bookingAuthenticationProvider(detailsService, encoder));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void authenticatesBothBcryptFormatsAndErasesCredentials(boolean prefixed) {
        User user = databaseUser("ACTIVE", prefixed ? "{bcrypt}" + hash : hash);
        when(repository.findByUsername("test-customer")).thenReturn(Optional.of(user));

        Authentication result = authenticate(password);

        assertTrue(result.isAuthenticated());
        DatabaseUserPrincipal principal = assertInstanceOf(DatabaseUserPrincipal.class, result.getPrincipal());
        assertEquals(42L, principal.getUserId());
        assertEquals("test-customer", principal.getUsername());
        assertTrue(principal.isEnabled());
        assertTrue(principal.getAuthorities().isEmpty());
        assertTrue(principal.getPassword() == null, "Principal credentials must be erased.");
        assertTrue(result.getCredentials() == null, "Token credentials must be erased.");
        verify(repository).findByUsername("test-customer");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsWrongPassword() {
        User user = databaseUser("ACTIVE", hash);
        when(repository.findByUsername("test-customer")).thenReturn(Optional.of(user));
        assertThrows(BadCredentialsException.class, () -> authenticate(UUID.randomUUID().toString()));
    }

    @Test
    void unknownUserHasSameGenericFailureAsWrongPassword() {
        when(repository.findByUsername("test-customer")).thenReturn(Optional.empty());
        BadCredentialsException unknown = assertThrows(BadCredentialsException.class,
                () -> authenticate(password));
        User user = databaseUser("ACTIVE", hash);
        when(repository.findByUsername("test-customer")).thenReturn(Optional.of(user));
        BadCredentialsException wrong = assertThrows(BadCredentialsException.class,
                () -> authenticate(UUID.randomUUID().toString()));
        assertEquals(wrong.getMessage(), unknown.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"INACTIVE", "DISABLED", "active", ""})
    void onlyExactActiveStatusIsEnabled(String status) {
        User user = databaseUser(status, hash);
        when(repository.findByUsername("test-customer")).thenReturn(Optional.of(user));
        assertThrows(DisabledException.class, () -> authenticate(password));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "plaintext-test-value", "{noop}test-value", "{argon2}invalid",
            "{bcrypt}invalid", "$2a$10$short", "$2a$03$invalid", "{BCRYPT}invalid"})
    void rejectsMalformedUnsupportedAndPlaintextStoredCredentials(String stored) {
        User user = databaseUser("ACTIVE", stored);
        when(repository.findByUsername("test-customer")).thenReturn(Optional.of(user));
        assertThrows(BadCredentialsException.class, () -> authenticate(password));
    }

    @Test
    void rejectsPlaintextEvenWhenItEqualsSubmittedPassword() {
        User user = databaseUser("ACTIVE", password);
        when(repository.findByUsername("test-customer")).thenReturn(Optional.of(user));
        assertThrows(BadCredentialsException.class, () -> authenticate(password));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{noop}", "{argon2}", "{BCRYPT}", "{bcrypt}{bcrypt}"})
    void rejectsUnsupportedPrefixesEvenWithValidBcryptPayload(String prefix) {
        User user = databaseUser("ACTIVE", prefix + hash);
        when(repository.findByUsername("test-customer")).thenReturn(Optional.of(user));
        assertThrows(BadCredentialsException.class, () -> authenticate(password));
    }

    @Test
    void rejectsMalformedBcryptWithTrailingData() {
        User user = databaseUser("ACTIVE", "{bcrypt}" + hash + "extra");
        when(repository.findByUsername("test-customer")).thenReturn(Optional.of(user));
        assertThrows(BadCredentialsException.class, () -> authenticate(password));
    }

    @Test
    void rejectsCaseMismatchedLookupResult() {
        User user = databaseUser("ACTIVE", hash);
        user.setUsername("TEST-CUSTOMER");
        when(repository.findByUsername("test-customer")).thenReturn(Optional.of(user));
        assertThrows(BadCredentialsException.class, () -> authenticate(password));
    }

    @Test
    void rejectsUserWithoutPersistedId() {
        User user = new User();
        user.setUsername("test-customer");
        when(repository.findByUsername("test-customer")).thenReturn(Optional.of(user));
        assertThrows(UsernameNotFoundException.class,
                () -> detailsService.loadUserByUsername("test-customer"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void invalidUsernameDoesNotAccessRepository(String username) {
        assertThrows(UsernameNotFoundException.class, () -> detailsService.loadUserByUsername(username));
        verifyNoInteractions(repository);
    }

    @Test
    void principalSupportsExplicitCredentialErasure() {
        DatabaseUserPrincipal principal = new DatabaseUserPrincipal(42L, "test-customer", hash, true);
        principal.eraseCredentials();
        assertTrue(principal.getPassword() == null);
        assertEquals(42L, principal.getUserId());
    }

    @Test
    void encoderRejectsNullInputAndDoesNotRequestMigration() {
        assertFalse(encoder.matches(null, hash));
        assertFalse(encoder.upgradeEncoding(hash));
        assertFalse(encoder.upgradeEncoding("{bcrypt}" + hash));
    }

    private Authentication authenticate(String suppliedPassword) {
        return manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                "test-customer", suppliedPassword));
    }

    private User databaseUser(String status, String storedHash) {
        User user = spy(new User());
        doReturn(42L).when(user).getId();
        user.setUsername("test-customer");
        user.setStatus(status);
        user.setPasswordHash(storedHash);
        return user;
    }
}
