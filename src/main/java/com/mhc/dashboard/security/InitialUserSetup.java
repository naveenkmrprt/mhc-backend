package com.mhc.dashboard.security;

import com.mhc.dashboard.models.User;
import com.mhc.dashboard.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InitialUserSetup implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Spring Boot automatically maps APP_BOOTSTRAP_USERNAME env var -> app.bootstrap.username
    @Value("${app.bootstrap.username:}")
    private String bootstrapUsername;

    @Value("${app.bootstrap.password:}")
    private String bootstrapPassword;

    @Override
    public void run(String... args) throws Exception {
        // Log what we see so Render logs confirm env vars are loaded
        System.out.println("[InitialUserSetup] bootstrapUsername present: " + !bootstrapUsername.isEmpty());
        System.out.println("[InitialUserSetup] bootstrapPassword present: " + !bootstrapPassword.isEmpty());
        System.out.println("[InitialUserSetup] current user count: " + userRepository.count());

        if (!bootstrapUsername.isEmpty() && !bootstrapPassword.isEmpty()) {
            // Use findByUsername so this is idempotent on every restart - upsert the bootstrap user
                existingUser -> {
                    System.out.println("[InitialUserSetup] Bootstrap user already exists: " + bootstrapUsername);
                    existingUser.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
                    userRepository.save(existingUser);
                    System.out.println("[InitialUserSetup] Bootstrap user password updated from environment variables.");
                },
                () -> {
                    User user = new User();
                    user.setUsername(bootstrapUsername);
                    user.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
                    user.setRole("ROLE_ADMIN");
                    userRepository.save(user);
                    System.out.println("[InitialUserSetup] Bootstrap user created: " + bootstrapUsername + " with role ROLE_ADMIN");
                }
            );
        } else {
            System.out.println("[InitialUserSetup] WARNING: APP_BOOTSTRAP_USERNAME or APP_BOOTSTRAP_PASSWORD env var is empty - no admin user will be created!");
        }
    }
}
