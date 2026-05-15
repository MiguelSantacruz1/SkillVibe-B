package com.skillvibe.tutoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // Habilita @Async para el Patrón Observer (NotificationEventListener)
public class TutoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(TutoringApplication.class, args);
    }

}
