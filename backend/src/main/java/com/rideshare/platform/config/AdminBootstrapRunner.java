package com.rideshare.platform.config;

import com.rideshare.platform.user.entity.User;
import com.rideshare.platform.user.entity.UserStatus;
import com.rideshare.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Bootstraps the first ADMIN account at startup. There is otherwise no way for any account to
 * ever obtain the ADMIN role - JWT roles come only from {@code user.isRoleAdmin()}, and nothing
 * else in the app can set that flag (a chicken-and-egg problem, same reason DriverController's
 * onboard endpoint can't be DRIVER-gated). Only runs if no admin exists yet, and only if both
 * env vars are set - no hardcoded default credentials, matching how JWT_SECRET/DB creds/
 * MAPPLS_API_KEY are already sourced from the environment in this app.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_BOOTSTRAP_EMAIL:}")
    private String bootstrapEmail;

    @Value("${ADMIN_BOOTSTRAP_PASSWORD:}")
    private String bootstrapPassword;

    @Value("${ADMIN_BOOTSTRAP_MOBILE:9999999999}")
    private String bootstrapMobile;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRoleAdminTrue()) {
            return;
        }
        if (bootstrapEmail.isBlank() || bootstrapPassword.isBlank()) {
            log.warn("No ADMIN account exists yet and ADMIN_BOOTSTRAP_EMAIL/ADMIN_BOOTSTRAP_PASSWORD are not set - "
                    + "admin-only endpoints are unreachable until an admin is bootstrapped or promoted.");
            return;
        }

        Optional<User> existing = userRepository.findByEmail(bootstrapEmail);
        User user = existing.orElseGet(() -> {
            User u = new User();
            u.setName("Admin");
            u.setEmail(bootstrapEmail);
            u.setMobile(bootstrapMobile);
            u.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
            u.setRolePassenger(true);
            u.setStatus(UserStatus.ACTIVE);
            return u;
        });
        user.setRoleAdmin(true);
        userRepository.save(user);
        log.info("Bootstrapped ADMIN account for {} ({})", bootstrapEmail, existing.isPresent() ? "promoted existing user" : "created");
    }
}
