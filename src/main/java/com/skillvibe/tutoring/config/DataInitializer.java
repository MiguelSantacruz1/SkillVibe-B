package com.skillvibe.tutoring.config;

import com.skillvibe.tutoring.model.Role;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Slf4j
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@skillvibe.com";
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = new User();
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setFullName("SkillVibe Administrator");
                admin.setRole(Role.ADMIN);
                admin.setBalance(0.0);
                admin.setCreatedAt(LocalDateTime.now());
                admin.setUpdatedAt(LocalDateTime.now());

                userRepository.save(admin);
                log.info("Usuario administrador por defecto creado: {}", adminEmail);
            } else {
                log.info("El usuario administrador ya existe.");
            }
        };
    }
}
