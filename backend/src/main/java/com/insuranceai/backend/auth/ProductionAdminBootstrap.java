package com.insuranceai.backend.auth;

import com.insuranceai.backend.auth.entity.Role;
import com.insuranceai.backend.auth.entity.User;
import com.insuranceai.backend.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Profile("prod")
public class ProductionAdminBootstrap implements ApplicationRunner {
    private static final UUID DEMO_ID = UUID.fromString("70975aff-125f-4c0b-952f-0477d412dfa4");
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String username;
    private final String email;
    private final String password;

    public ProductionAdminBootstrap(UserRepository users, PasswordEncoder encoder,
            @Value("${APP_BOOTSTRAP_ADMIN_USERNAME}") String username,
            @Value("${APP_BOOTSTRAP_ADMIN_EMAIL}") String email,
            @Value("${APP_BOOTSTRAP_ADMIN_PASSWORD}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateConfiguration();
        User existing = users.findByUsername(username).orElse(null);
        if (existing != null) {
            if (existing.getRole() != Role.ADMIN || !existing.isEnabled() || !email.equals(existing.getEmail())) {
                throw new IllegalStateException("Production admin bootstrap: configured username must belong to an enabled ADMIN with the configured email; resolve the account conflict explicitly.");
            }
        } else {
            if (users.existsByEmail(email)) {
                throw new IllegalStateException("Production admin bootstrap: configured email already belongs to another account.");
            }
            User admin = new User();
            admin.setUsername(username);
            admin.setEmail(email);
            admin.setPasswordHash(encoder.encode(password));
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            users.save(admin);
        }

        users.findById(DEMO_ID).filter(demo -> "admin".equals(demo.getUsername())
                && "admin@insuranceai.local".equals(demo.getEmail())
                && demo.isEnabled()
                && encoder.matches("admin123", demo.getPasswordHash()))
                .ifPresent(demo -> {
                    demo.setEnabled(false);
                    users.save(demo);
                });
    }

    private void validateConfiguration() {
        if (username == null || username.isBlank() || !username.equals(username.trim())
                || username.length() > 100 || "admin".equalsIgnoreCase(username)) {
            throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_USERNAME must be nonblank, at most 100 characters, without surrounding whitespace, and different from admin.");
        }
        if (email == null || email.isBlank() || email.length() > 255
                || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_EMAIL must be a valid email address of at most 255 characters.");
        }
        // BCrypt limits input to 72 bytes. Reject weak repeated-character values as well.
        if (password == null || password.isBlank() || password.length() < 16
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || password.chars().distinct().count() < 8) {
            throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_PASSWORD must contain at least 16 characters and 8 distinct characters, and fit within BCrypt's 72-byte limit.");
        }
    }
}
