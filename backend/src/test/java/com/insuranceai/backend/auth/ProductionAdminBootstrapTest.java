package com.insuranceai.backend.auth;

import com.insuranceai.backend.auth.entity.Role;
import com.insuranceai.backend.auth.entity.User;
import com.insuranceai.backend.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductionAdminBootstrapTest {
    private static final UUID DEMO_ID = UUID.fromString("70975aff-125f-4c0b-952f-0477d412dfa4");
    private static final String PASSWORD = "Bootstrap-test!42-secret";
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final ProductionAdminBootstrap bootstrap = bootstrap("operator", "operator@example.test", PASSWORD);

    private ProductionAdminBootstrap bootstrap(String name, String email, String password) {
        return new ProductionAdminBootstrap(users, encoder, name, email, password);
    }

    private User account(String name, String email, String password) {
        User user = new User();
        user.setUsername(name);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setRole(Role.ADMIN);
        return user;
    }

    private User existingAdmin() {
        User admin = account("operator", "operator@example.test", "Previously-changed!password");
        when(users.findByUsername("operator")).thenReturn(Optional.of(admin));
        return admin;
    }

    @Test
    void createsAdminOnceAndPreservesPasswordOnSubsequentStartup() {
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            when(users.findByUsername("operator")).thenReturn(Optional.of(saved));
            return saved;
        });
        bootstrap.run(null);
        bootstrap.run(null);
        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users, times(1)).save(captor.capture());
        User created = captor.getValue();
        assertThat(created.getRole()).isEqualTo(Role.ADMIN);
        assertThat(created.isEnabled()).isTrue();
        assertThat(created.getEmail()).isEqualTo("operator@example.test");
        assertThat(encoder.matches(PASSWORD, created.getPasswordHash())).isTrue();
    }

    @Test
    void preservesExistingAdminPassword() {
        User admin = existingAdmin();
        String hash = admin.getPasswordHash();
        bootstrap.run(null);
        assertThat(admin.getPasswordHash()).isEqualTo(hash);
        verify(users, never()).save(any());
    }

    @Test
    void disablesUntouchedSeedIdempotently() {
        existingAdmin();
        User demo = account("admin", "admin@insuranceai.local", "admin123");
        when(users.findById(DEMO_ID)).thenReturn(Optional.of(demo));
        bootstrap.run(null);
        bootstrap.run(null);
        assertThat(demo.isEnabled()).isFalse();
        verify(users, times(1)).save(demo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"username", "email", "password", "uuid"})
    void preservesAlteredDemoOrDifferentAccount(String changed) {
        existingAdmin();
        User demo = account("admin", "admin@insuranceai.local", "admin123");
        switch (changed) {
            case "username" -> demo.setUsername("renamed");
            case "email" -> demo.setEmail("changed@example.test");
            case "password" -> demo.setPasswordHash(encoder.encode(PASSWORD));
            case "uuid" -> when(users.findById(UUID.randomUUID())).thenReturn(Optional.of(demo));
            default -> throw new AssertionError();
        }
        if (!changed.equals("uuid")) {
            when(users.findById(DEMO_ID)).thenReturn(Optional.of(demo));
        }
        bootstrap.run(null);
        assertThat(demo.isEnabled()).isTrue();
        verify(users, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"role", "disabled", "email"})
    void failsOnExistingAccountConflictWithoutMutations(String conflict) {
        User admin = existingAdmin();
        switch (conflict) {
            case "role" -> admin.setRole(Role.CUSTOMER);
            case "disabled" -> admin.setEnabled(false);
            case "email" -> admin.setEmail("other@example.test");
            default -> throw new AssertionError();
        }
        assertThatThrownBy(() -> bootstrap.run(null)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("enabled ADMIN").hasMessageNotContaining(PASSWORD);
        verify(users, never()).save(any());
        verify(users, never()).findById(any());
    }

    @Test
    void failsIfEmailIsOwnedByAnotherAccount() {
        when(users.existsByEmail("operator@example.test")).thenReturn(true);
        assertThatThrownBy(() -> bootstrap.run(null)).hasMessageContaining("email already belongs");
        verify(users, never()).save(any());
        verify(users, never()).findById(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "aaaaaaaaaaaaaaaa", "                                                                        ",
            "A-very-long-password-that-exceeds-the-bcrypt-seventy-two-byte-limit-1234567890"})
    void rejectsInvalidPasswordBeforeRepositoryAccess(String password) {
        assertThatThrownBy(() -> bootstrap("operator", "operator@example.test", password).run(null))
                .hasMessageContaining("APP_BOOTSTRAP_ADMIN_PASSWORD");
        verifyNoInteractions(users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "ADMIN", "", " operator"})
    void rejectsUnsafeUsername(String username) {
        assertThatThrownBy(() -> bootstrap(username, "operator@example.test", PASSWORD).run(null))
                .hasMessageContaining("APP_BOOTSTRAP_ADMIN_USERNAME");
        verifyNoInteractions(users);
    }

    @Test
    void rejectsInvalidEmail() {
        assertThatThrownBy(() -> bootstrap("operator", "invalid", PASSWORD).run(null))
                .hasMessageContaining("APP_BOOTSTRAP_ADMIN_EMAIL");
        verifyNoInteractions(users);
    }
}
