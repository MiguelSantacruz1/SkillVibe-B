package com.skillvibe.tutoring.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.skillvibe.tutoring.model.Role;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // El usuario administrador por defecto ya no se crea automáticamente.
        };
    }
}
