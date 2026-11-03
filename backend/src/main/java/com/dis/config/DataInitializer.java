package com.dis.config;

import com.dis.model.AppUser;
import com.dis.model.enums.UserRole;
import com.dis.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password:admin123}")
    private String adminPassword;

    @Value("${app.seed.pharmacist-password:pharma123}")
    private String pharmacistPassword;

    @Value("${app.seed.warehouse-password:warehouse123}")
    private String warehousePassword;

    @Override
    public void run(String... args) {
        initUser("admin", adminPassword, "admin@hospital.org", UserRole.ADMIN);
        initUser("pharmacist", pharmacistPassword, "pharmacist@hospital.org", UserRole.PHARMACIST);
        initUser("warehouse", warehousePassword, "warehouse@hospital.org", UserRole.WAREHOUSE);
    }

    private void initUser(String username, String rawPassword, String email, UserRole role) {
        if (!userRepository.existsByUsername(username)) {
            AppUser user = AppUser.builder()
                    .username(username)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .email(email)
                    .role(role)
                    .active(true)
                    .build();
            userRepository.save(user);
            log.info("Initialized default seed user '{}' with role '{}'", username, role);
        }
    }
}
