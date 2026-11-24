package com.libraflow.config;

import com.libraflow.model.AppUser;
import com.libraflow.model.Book;
import com.libraflow.model.Role;
import com.libraflow.repository.AppUserRepository;
import com.libraflow.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Populates initial catalog and optional users from environment variables on startup.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DataInitializer.class);

    private final AppUserRepository userRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Value("${app.seed.admin-username:admin}")
    private String adminUsername;

    @Value("${app.seed.admin-password:}")
    private String adminPassword;

    @Value("${app.seed.member-username:etudiant}")
    private String memberUsername;

    @Value("${app.seed.member-password:}")
    private String memberPassword;

    public DataInitializer(AppUserRepository userRepository,
                           BookRepository bookRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            LOG.info("Data seeding disabled (app.seed.enabled=false)");
            return;
        }

        seedUserIfConfigured(adminUsername, adminPassword, "Bibliothecaire Admin", Role.ADMIN);
        seedUserIfConfigured(memberUsername, memberPassword, "Etudiant L3 DSI", Role.MEMBER);

        if (bookRepository.count() == 0) {
            bookRepository.saveAll(List.of(
                    new Book("The DevOps Handbook", "Gene Kim", "978-1942788003", 3),
                    new Book("Continuous Delivery", "Jez Humble & David Farley", "978-0321601919", 2),
                    new Book("Clean Code", "Robert C. Martin", "978-0132350884", 4),
                    new Book("Designing Data-Intensive Applications", "Martin Kleppmann", "978-1449373320", 2)
            ));
            LOG.info("Seeded initial library catalog with 4 books");
        }
    }

    private void seedUserIfConfigured(String username, String rawPassword, String fullName, Role role) {
        if (rawPassword == null || rawPassword.isBlank()) {
            LOG.warn("Skipping seed for user '{}' because password env var is not set", username);
            return;
        }
        String normalized = username.trim().toLowerCase();
        if (!userRepository.existsByUsername(normalized)) {
            userRepository.save(new AppUser(normalized, passwordEncoder.encode(rawPassword), fullName, role));
            LOG.info("Seeded {} account: username='{}'", role, normalized);
        }
    }
}
